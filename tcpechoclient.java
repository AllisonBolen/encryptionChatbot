import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

class tcpechoclient{
    public static void main(String args[]){
	try{
	    SocketChannel sc = SocketChannel.open();
	    sc.connect(new InetSocketAddress("127.0.0.1",9877));
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
