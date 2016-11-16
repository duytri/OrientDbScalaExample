package main.scala

import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.{ Direction, Edge, Vertex }
import com.tinkerpop.blueprints.impls.orient._

import java.util.HashSet
import java.lang.Iterable

object OrientDbScalaExample {
  def main(args: Array[String]): Unit = {

    val WorkEdgeLabel = "Work"

    // opens the DB (if not existing, it will create it)
    val uri: String = "plocal:/home/duytri/orientdb/databases/scala_sample"
    val factory: OrientGraphFactory = new OrientGraphFactory(uri)
    val graph: OrientGraph = factory.getTx()

    try {

      // if the database does not contain the classes we need (it was just created),
      // then adds them
      if (graph.getVertexType("Person") == null) {

        // we now extend the Vertex class for Person and Company
        val person: OrientVertexType = graph.createVertexType("Person")
        person.createProperty("firstName", OType.STRING)
        person.createProperty("lastName", OType.STRING)

        val company: OrientVertexType = graph.createVertexType("Company")
        company.createProperty("name", OType.STRING)
        company.createProperty("revenue", OType.LONG)

        val project: OrientVertexType = graph.createVertexType("Project")
        project.createProperty("name", OType.STRING)

        // we now extend the Edge class for a "Work" relationship
        // between Person and Company
        val work: OrientEdgeType = graph.createEdgeType(WorkEdgeLabel)
        work.createProperty("startDate", OType.DATE)
        work.createProperty("endDate", OType.DATE)
        work.createProperty("projects", OType.LINKSET)

        graph.commit()

      } else {
        // cleans up the DB since it was already created in a preceding run
        graph.command(new OCommandSQL("DELETE VERTEX V")).execute()
        graph.command(new OCommandSQL("DELETE EDGE E")).execute()
        graph.commit()
      }

      // adds some people
      // (we have to force a vararg call in addVertex() method to avoid ambiguous
      // reference compile error, which is pretty ugly)
      val johnDoe: Vertex = graph.addVertex("class:Person", Nil: _*)
      johnDoe.setProperty("firstName", "John")
      johnDoe.setProperty("lastName", "Doe")
      graph.commit()

      // we can also set properties directly in the constructor call
      val johnSmith: Vertex = graph.addVertex("class:Person", "firstName", "John", "lastName", "Smith")
      val janeDoe: Vertex = graph.addVertex("class:Person", "firstName", "Jane", "lastName", "Doe")
      graph.commit()

      // creates a Company
      val acme: Vertex = graph.addVertex("class:Company", "name", "ACME", "revenue", "10000000")
      graph.commit()

      // creates a couple of projects
      val acmeGlue: Vertex = graph.addVertex("class:Project", "name", "ACME Glue")
      val acmeRocket: Vertex = graph.addVertex("class:Project", "name", "ACME Rocket")
      graph.commit()

      // creates edge JohnDoe worked for ACME
      val johnDoeAcme: Edge = graph.addEdge(null, johnDoe, acme, WorkEdgeLabel)
      johnDoeAcme.setProperty("startDate", "2010-01-01")
      johnDoeAcme.setProperty("endDate", "2013-04-21")
      var hsProjs = new HashSet[Vertex]()
      hsProjs.add(acmeGlue)
      hsProjs.add(acmeRocket)
      johnDoeAcme.setProperty("projects", hsProjs)
      graph.commit()

      // another way to create an edge, starting from the source vertex
      val johnSmithAcme: Edge = johnSmith.addEdge(WorkEdgeLabel, acme)
      johnSmithAcme.setProperty("startDate", "2009-01-01")
      graph.commit()

      // prints all the people who works/worked for ACME
      val res: Iterable[OrientVertex] = graph
        .command(new OCommandSQL(s"SELECT expand(in('${WorkEdgeLabel}')) FROM Company WHERE name='ACME'"))
        .execute()

      println("ACME people:")
      val result = res.iterator()
      while (result.hasNext()) {
        val person = result.next()
        val workEdgeIterator = person.getEdges(Direction.OUT, WorkEdgeLabel).iterator()
        val edge = workEdgeIterator.next()

        // retrieves worker's info
        val status = if (edge.getProperty("endDate") != null) "retired" else "active"

        val iterVertex: Iterable[Vertex] = edge.getProperty("projects")

        var projects = ""
        if (iterVertex != null) {
          val setVertex = iterVertex.iterator();
          while (setVertex.hasNext()) {
            val vertex = setVertex.next()
            projects += vertex.getProperty("name").toString() + ", ";
          }
          projects = projects.substring(0, projects.length() - 2);
        } else
          projects = "Any project"

        // and prints them
        System.out.println("Name: " + person.getProperty("lastName") + " " + person.getProperty("firstName")
          + ", " + status + ", Worked on: " + projects + ".")
      }
    } catch {
      case t: Throwable => {
        println("************************ ERROR ************************")
        t.printStackTrace() // TODO: handle error
        println()
        println("************************ ERROR ************************")
      }
    } finally {
      graph.shutdown()
      factory.close()
    }

  }
}