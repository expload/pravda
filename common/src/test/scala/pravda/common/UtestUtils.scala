package pravda.common

import java.io.File

import utest.framework._
import utest._

object UtestUtils {

  def constructTestsFromDir[T, U](dir: File,
                                  ext: String,
                                  processFile: File => Either[String, U],
                                  test: U => T,
                                  target: U => T,
                                  assertT: (T, T) => Unit): utest.Tests = {

    def walkDir(prefix: String, dir: File): Option[(Tree[String], TestCallTree)] =
      if (dir.isDirectory) {
        val files = dir.listFiles.filter(f => f.isFile && f.getName.endsWith(s".$ext"))
        val fileNames = files.map(f => Tree(f.getName.stripSuffix(s".$ext")))
        val fileCalls = files.map(f =>
          new TestCallTree(Left {
            processFile(f) match {
              case Left(err) => Predef.assert(false, err)
              case Right(p) => assertT(test(p), target(p))
            }
          }))

        val dirs = dir.listFiles.filter(_.isDirectory)
        val (dirTrees, dirCallTrees) = dirs.flatMap(d => walkDir(d.getName, d)).unzip

        if (files.nonEmpty || dirTrees.nonEmpty) {
          Some((Tree(prefix, fileNames ++ dirTrees: _*), new TestCallTree(Right(fileCalls ++ dirCallTrees))))
        } else {
          None
        }
      } else {
        None
      }

    val (tree, callTree) = walkDir("", dir).getOrElse((Tree(""), new TestCallTree(Right(IndexedSeq.empty))))
    Tests(tree, callTree)
  }
}
