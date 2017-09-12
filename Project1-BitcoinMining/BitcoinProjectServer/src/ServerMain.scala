
/**
 * @author Universe_Lu
 */
import akka.actor._
import akka.routing.RoundRobinRouter
import scala.collection.mutable.ArrayBuffer
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.ArrayBuffer
import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Message
case class Register(client:ActorRef)
case class Result(str:String, coin:String, actor:ActorRef)
case object StopWork
case object ShutDown
case class Workload(preString : String, numOfZeros: Int)
case object RegisterAck



// define the actor of Server
class Server(preFix: String, numOfZeros: Int, sec:Int) extends Actor { 
  
    println("Server startes." + self.path + "     YuzhouLu")
    val WorkerIDLength = 3
    val WorkerIDString:ArrayBuffer[String] = GenerateWorkerID(WorkerIDLength)
    val name = "YuzhouLu"
    var workerNum = 0
    var resultNum = 0
    var workers = ArrayBuffer[ActorRef]()
 
  def GenerateWorkerID(length:Int):ArrayBuffer[String]={
    var strs:ArrayBuffer[String] = ArrayBuffer[String]()
    var strBuf = new StringBuilder("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    FindWorkerID(strs,strBuf, 1,length)
    return strs
  }
  
  def FindWorkerID(strs:ArrayBuffer[String],strBuf:StringBuilder,start:Int,length:Int):Unit = {
    if( start > length) return
    var ch:Char = '0'
    while(ch <= '9'){
      strBuf.setCharAt(start,ch)
      if( start == length )
        strs.append(strBuf.substring(1,length+1))
      FindWorkerID(strs,strBuf,start+1, length)
      ch = (ch.toInt + 1).toChar
    }
  }
    
     def receive = {
      case Register(client)=>
        workers.append(client)
        workerNum += 1
          client ! println (" No." + workerNum + " worker will start working!")
          client ! Workload(name+WorkerIDString((workerNum - 1)%WorkerIDString.length),numOfZeros)
       
      case Result(str,coin,actor)=>
        resultNum += 1
        println(str+"\t\t" +coin)
        
      case StopWork=>
       // stopAllClients()
        var i = 0
        for( i <- 0 until workers.length)  { workers(i) ! ShutDown }
        println("----------------------------------------------------------------------------------")
        println("After calculation of " + sec +" seconds with " + workerNum +"  workers ," )
        println("")
        println("System has found " + resultNum +" bit coins" +" starting with " + numOfZeros +" 0s")
       // server ! ShutDown(self)
        context stop self
    }
}



//define the actor of serverworker
class ServerWorker(server: ActorRef) extends Actor {
  
  var preStr:String = ""
  var numOfZeros:Int = 1
  val SubStringLength:Int = 4
  var strBuf = new StringBuilder("-------------------------------------------------")
  
  server ! Register(self)
  
  def startwork()= {
    var i = 1
    for( i <- 1 to SubStringLength)
    GetFullWorkerCoin(1,i)
   //Get the full worker string
   def GetFullWorkerCoin(start:Int,length:Int):Unit = {
    if( start > length) return
    var ch:Char = 'A'
    while(ch <= 'z'){
      strBuf.setCharAt(start,ch)
      if( start == length )
         GetHashString(length)
      GetFullWorkerCoin(start+1, length)
      ch = (ch.toInt + 1).toChar
    }
   }
  }
  
// get the hash strings with the XX numbers of 0's
  def GetHashString(length:Int) = {
    var shaStr = preStr + strBuf.substring(1,length+1)
 // get the HASH value 
    var coin = MessageDigest.getInstance("SHA-256")
    var bitcoin:String=coin.digest(shaStr.getBytes).foldLeft("")(
         (s:String, b: Byte) => 
           s + Character.forDigit((b & 0xf0) >> 4, 16) +Character.forDigit(b & 0x0f, 16)
         )
    var i = 0
    var testOk = true
    while(i < numOfZeros){
      if(bitcoin.charAt(i) != '0' )
        testOk = false
      i += 1
    }
    if(testOk == true){
      server ! Result(shaStr,bitcoin,self)
     //context.actorSelection("akka.tcp://serverSys@192.168.0.11:5150/user/server") ! Result(shaStr,bitcoin,self)
    }
  }
  
  def receive={
    case Workload(str, zeros)=>
      this.preStr = str
      this.numOfZeros = zeros
      startwork()
    case ShutDown=>
      //println("1")
      context stop self
  }
  
}



  
object ServerMain {
  
  def main(args: Array[String]): Unit = {

   val serverSystem = ActorSystem("serverSys", ConfigFactory.load(ConfigFactory.parseString("""
  akka {
    actor {
      provider = "akka.remote.RemoteActorRefProvider"
    }
    remote {
      enabled-transports = ["akka.remote.netty.tcp"]
      transport = "akka.remote.netty.NettyRemoteTransport"
      netty.tcp {
        hostname = "192.168.0.11"
        port = 5150
      }
    }
    log-dead-letters = off
  }
  """)))
    //set the running time and number of zeros , you can change it by yourself
    var second = 30
    var NumOfZeros = 7
    println("The number of prefix zeros in coins is set to be " + NumOfZeros)
    println("The running time is set to be: " + second + " seconds") 
    // start the server
    val server = serverSystem.actorOf(Props(classOf[Server], "YuzhouLu", NumOfZeros, second), name = "server")
    //start 4 calculation client in each machine
    val serverworker1 = serverSystem.actorOf(Props(classOf[ServerWorker], server), name = "worker1")
    val serverworker2 = serverSystem.actorOf(Props(classOf[ServerWorker], server), name = "worker2")
    val serverworker3 = serverSystem.actorOf(Props(classOf[ServerWorker], server), name = "worker3")
    val serverworker4 = serverSystem.actorOf(Props(classOf[ServerWorker], server), name = "worker4")
    //val serverworker5 = serverSystem.actorOf(Props(classOf[ServerWorker], server), name = "worker5")
    //val serverworker6 = serverSystem.actorOf(Props(classOf[ServerWorker], server), name = "worker6")
    //val serverworker7 = serverSystem.actorOf(Props(classOf[ServerWorker], server), name = "worker7")
    //val serverworker8 = serverSystem.actorOf(Props(classOf[ServerWorker], server), name = "worker8")
    // when time is out ,stop the server 
    import serverSystem.dispatcher
    serverSystem.scheduler.scheduleOnce(Duration(second, TimeUnit.SECONDS), server, StopWork)

  }
}
