package io.github.mtomko.fqcount

import java.nio.file.Path

import cats.effect.{Blocker, Concurrent, Console, ContextShift, ExitCode, IO, Sync}
import cats.effect.Console.implicits._
import cats.implicits._
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import fs2.{compression, io, text, Pipe, Stream}

object FqCount
  extends CommandIOApp(
    name = "fq-count",
    header = "Counts reads in FASTQ files",
    version = BuildInfo.version
  ) {

  private[this] val BufferSize = 8192

  private[this] val fastqOpt = Opts.option[Path]("fastq", short = "f", help = "The FASTQ file containing reads")

  final case class Fastq(id: String, seq: String, id2: String, qual: String) {
    override def toString: String = id + "\n" + seq + "\n" + id2 + "\n" + qual + "\n"
  }

  override def main: Opts[IO[ExitCode]] = fastqOpt.map(run[IO](_).compile.drain.as(ExitCode.Success))

  def run[F[_]: Sync: Concurrent: ContextShift: Console](p: Path): Stream[F, Unit] =
    for {
      implicit0(blocker: Blocker) <- Stream.resource(Blocker[F])
      count <- fastq(p).as(1).foldMonoid
      _ <- Stream.eval(Console[F].putStrLn(count))
    } yield ()

  object stream {

    def fastq[F[_]]: Pipe[F, String, Fastq] =
      _.chunkN(4, allowFewer = false).map(seg => Fastq(seg(0), seg(1), seg(2), seg(3)))

    def lines[F[_]: Sync: Concurrent: ContextShift](p: Path)(implicit blocker: Blocker): Stream[F, String] = {
      val byteStream: Stream[F, Byte] =
        if (isGzFile(p))
          io.file.readAll[F](p, blocker, BufferSize).through(compression.gunzip(BufferSize)).flatMap(_.content)
        else io.file.readAll[F](p, blocker, BufferSize)
      byteStream
        .through(text.utf8Decode)
        .through(text.lines)
    }

  }

  def fastq[F[_]: Sync: Concurrent: ContextShift](path: Path)(implicit blocker: Blocker): Stream[F, Fastq] =
    stream.lines[F](path).through(stream.fastq)

  def isGzFile(p: Path): Boolean = p.getFileName.toString.toLowerCase.endsWith(".gz")

}
