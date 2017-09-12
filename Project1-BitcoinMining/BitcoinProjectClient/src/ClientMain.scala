
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

// Define the message 
case class Register(client:ActorRef)
case class Result(str:String, coin:String, actor:ActorRef)
case object StopWork
case object ShutDown
case class Workload(preString : String, numOfZeros: Int)


// define the actor of client
class Client(server: ActorSelection) extends Actor {
  
  var preStr:String = ""
  var numOfZeros:Int = 1
  val SubStringLength:Int = 4
  var strBuf = new StringBuilder("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
  
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
      //server ! Result(shaStr,bitcoin,self)
     context.actorSelection("akka.tcp://serverSys@192.168.0.11:5150/user/server") ! Result(shaStr,bitcoin,self)
    }
 }
  
  def receive={
    case Workload(str, zeros)=>
      this.preStr = str
      println("I am the worker of : "+str + ", The calculation is being in progress.")
      this.numOfZeros = zeros
      startwork()
    case ShutDown=>
      println("server is down, calculation done")
      context stop self
  }
}


object ClientMain {

  def main(args: Array[String]): Unit = {
 
    val clientSystem = ActorSystem("client", ConfigFactory.load(ConfigFactory.parseString("""
   akka {
    actor {
      provider = "akka.remote.RemoteActorRefProvider"
     
    }
      transport = "akka.remote.netty.NettyRemoteTransport"
      netty.tcp {
          hostname = "192.168.0.11"
          port = 5150
       }   
 }
  """)))   
    // my server's IP address and port. When you run the program, you should set your own ip address and port
    var remoteIP = "192.168.0.11" 
    var port = "5150"
    val remotePath = "akka.tcp://serverSys@"+remoteIP+":"+port+"/user/server"
    println(remotePath)
    
    val remoteServer = clientSystem.actorSelection(remotePath)
    println(remoteServer.pathString)
    
    val clientworker1 = clientSystem.actorOf(Props(classOf[Client], remoteServer), name = "worker1")
    val clientworker2 = clientSystem.actorOf(Props(classOf[Client], remoteServer), name = "worker2")
    val clientworker3 = clientSystem.actorOf(Props(classOf[Client], remoteServer), name = "worker3")
    val clientworker4 = clientSystem.actorOf(Props(classOf[Client], remoteServer), name = "worker4")

  }
}
  