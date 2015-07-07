package info.fotm.clustering.RMClustering

/**
 * Created by Admin on 04.07.2015.
 */
object FileWriter
{
  def apply(content: String, append: Boolean = true): Unit =
  {

    import java.io._

    val fw = new FileWriter("f:\\output.txt", append); //the true will append the new data
    //val pw = new PrintWriter(fw)
    try
    {
      fw.write(content.replace("List","\nList")
        .replace("MathVector","\nMathV")
        .replace("Vector","\nVector"))
      //pw.print(content) //appends the string to the file
    }
    finally
    {
      //pw.close()
      fw.close()
    }

    //    val pw = new PrintWriter(new File("f:\\input.txt"))
    //    pw.write(content)
    //    pw.close
  }
}
