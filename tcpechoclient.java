import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Scanner;

class tcpechoclient{
    public static void main(String args[]){
	Scanner scan = new Scanner(System.in);
	try{
	    System.out.println("What is the server's IP address?");
	    String IP = scan.nextLine();
	    System.out.println("What is the server's port?");
	    int portNum = scan.nextInt();
	    SocketChannel sc = SocketChannel.open();
	    sc.connect(new InetSocketAddress(IP,portNum));
	    Console cons = System.console();
	    String m = cons.readLine("Enter your message: ");
	    ByteBuffer buf = ByteBuffer.wrap(m.getBytes());
	    sc.write(buf);
	    ByteBuffer buf2 = ByteBuffer.allocate(5000);
	    sc.read(buf2);
	    buf2.flip();
	    byte[] a = new byte[buf2.remaining()];
	    buf2.get(a);
	    String message = new String(a);
	    System.out.println("Got from server: "+message);
	    sc.close();
	}catch(IOException e){
	    System.out.println("Got an Exception");
	}
    }
}
