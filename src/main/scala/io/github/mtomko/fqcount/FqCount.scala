package io.github.mtomko.fqcount

import java.nio.file.Path

import cats.effect.{Blocker, Concurrent, Console, ContextShift, ExitCode, IO, Sync}
import cats.effect.Console.implicits._
import cats.implicits._
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import fs2.{compression, io, text, Chunk, Pipe, Stream}

object FqCount
  extends CommandIOApp(
    name = "fq-count",
    header = "Counts reads in FASTQ files",
    version = BuildInfo.version
  ) {

  private[this] val bufferSize = 8192

  private[this] val fastqOpt = Opts.option[Path]("fastq", short = "f", help = "The FASTQ file containing reads")

  override def main: Opts[IO[ExitCode]] = fastqOpt.map(run[IO](_).compile.drain.as(ExitCode.Success))

  def run[F[_]: Sync: Concurrent: ContextShift: Console](p: Path): Stream[F, Unit] =
    Stream.resource(Blocker[F]).flatMap { implicit blocker: Blocker =>
      io.file
        .readAll[F](p, blocker, bufferSize)
        .mapChunks(c => Chunk(new String(c.toBytes.toArray, "ASCII")))
        .through(text.lines)
        .through(stream.fastq[F]) // equivalent to _.chunkN(4, allowFewer = false).as(1)
        .foldMonoid // adds up the 1s emitted above
        .evalMap(i => Console[F].putStrLn(i))
    }

  object stream {

    def fastq[F[_]]: Pipe[F, String, Int] = _.chunkN(4, allowFewer = false).as(1)

    def byteStream[F[_]: Sync: Concurrent: ContextShift](p: Path)(implicit blocker: Blocker): Stream[F, Byte] = {
      val s = io.file.readAll[F](p, blocker, bufferSize)
      if (isGzFile(p)) s.through(compression.gunzip(bufferSize)).flatMap(_.content)
      else s
    }

    private[this] def isGzFile(p: Path): Boolean = p.getFileName.toString.toLowerCase.endsWith(".gz")

  }

}
