import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Scanner;

class tcpechoserver{
    public static void main(String args[]){
	Scanner scan = new Scanner(System.in);
	try{
	    System.out.println("Enter a port for the server to run on: ");
	    int port = scan.nextInt();
	    ServerSocketChannel c = ServerSocketChannel.open();
	    c.bind(new InetSocketAddress(port));
	    while(true){
		SocketChannel sc = c.accept();
		System.out.println("Client Connected: " + sc.getRemoteAddress());
		TcpServerThread t = new TcpServerThread(sc);
		t.start();
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
