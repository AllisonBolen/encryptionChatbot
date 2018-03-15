import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap; // https://stackoverflow.com/questions/2836267/concurrenthashmap-in-java

class tcpechoserver{
    public static void main(String args[]){
	Scanner scan = new Scanner(System.in);
	// map to store all connected client ips and threads
	ConcurrentHashMap<SocketAddress, Integer> clientMap = new ConcurrentHashMap<SocketAddress, Integer>();
	try{
	    System.out.println("Enter a port for the server to run on: ");
	    int port = scan.nextInt();
	    ServerSocketChannel c = ServerSocketChannel.open();
	    c.bind(new InetSocketAddress(port));
	    int count = 0;
	    while(true){
			SocketChannel sc = c.accept();
            if (!clientMap.containsKey(sc.getRemoteAddress())) {
                clientMap.putIfAbsent(sc.getRemoteAddress(), count);
            }
			System.out.println("Client Connected: " + sc.getRemoteAddress());
			TcpServerThread t = new TcpServerThread(sc);
			t.start();
			System.out.println(clientMap.toString());
			count++;
	    }
	} catch(IOException e){
	    System.out.println("Got an IO Exception");
	}
    }
}

class TcpServerThread extends Thread{
    SocketChannel sc;
    TcpServerThread(SocketChannel channel){
	sc = channel;
    }
    public void run(){
	// main method ? 
	try{
	  while(true){
	    ByteBuffer buffer = ByteBuffer.allocate(4096);
	    sc.read(buffer);
	    buffer.flip();
	    byte[] a = new byte[buffer.remaining()];
	    buffer.get(a);
	    String message = new String(a);
	    System.out.println("Got from client: "+message);
	    buffer.rewind();
	    sc.write(buffer);
	    //sc.close();
	  }
	}catch (IOException e ){
	    // print error
	    System.out.println("Got an IO Exception");
	}
	
    }
}
