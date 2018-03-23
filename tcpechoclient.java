import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Scanner;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import javax.xml.bind.DatatypeConverter;

class tcpechoclient {
    public static void main(String args[]) {
        Scanner scan = new Scanner(System.in);
        // set pub key and create sym key
        cryptotest ct = new cryptotest();
        ct.setPublicKey("RSApub.der");
        SecretKey s = ct.generateAESKey();
        byte encryptedsecret[] = ct.RSAEncrypt(s.getEncoded());
        SecureRandom r = new SecureRandom();
        byte ivBytes[] = new byte[16];
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        r.nextBytes(ivBytes);

        try {
            System.out.println("What is the server's IP address?");
            String IP = scan.nextLine();
            System.out.println("What is the server's port?");
            int portNum = scan.nextInt();
            SocketChannel sc = SocketChannel.open();
            sc.connect(new InetSocketAddress(IP, portNum));
            System.out.println("Connected to Chat Server");

            // send initial sym key to server for this client
            ByteBuffer buffer = ByteBuffer.wrap(ivBytes).wrap(encryptedsecret);
            sc.write(buffer);

            // create thread for this client
            TcpClientThread t = new TcpClientThread(sc);
            t.start();
            while (true) {
                // create random iv params
                SecureRandom r2 = new SecureRandom();
                byte ivBytes2[] = new byte[16];
                IvParameterSpec iv2 = new IvParameterSpec(ivBytes);
                r2.nextBytes(ivBytes2);

                Console cons = System.console();
                String m = cons.readLine("Enter your message: ");

                if (m.equals("Quit")) {
                    byte ciphertext[] = ct.encrypt(m.getBytes(), s, iv2);
                    ByteBuffer reffub = ByteBuffer.wrap(ivBytes2).wrap(ciphertext);
                    sc.write(reffub);
                    sc.close();
                    System.exit(0);
                } else {
                    // we need to send the sym key over at some point along with the iv
                    byte ciphertext[] = ct.encrypt(m.getBytes(), s, iv2);
                    ByteBuffer reffub = ByteBuffer.wrap(ivBytes2).wrap(ciphertext);
                    sc.write(reffub);
                }
            }
        } catch (IOException e) {
            System.out.println("Got an Exception");
        }
    }
}

class TcpClientThread extends Thread {
    SocketChannel sc;

    TcpClientThread(SocketChannel channel) {
        sc = channel;
    }

    public void run() {
        // main method ?
        try {
            while (true) {
                ByteBuffer buf2 = ByteBuffer.allocate(5000);
                sc.read(buf2);

                buf2.flip();
                byte[] a = new byte[buf2.remaining()];
                buf2.get(a);
                String message = new String(a);
                System.out.println("\nGot from server: " + message);
                if (message.equals("Quit")) {
                    ByteBuffer buf = ByteBuffer.wrap(message.getBytes());
                    sc.write(buf);
                    sc.close();
                    System.exit(0);
                }
                //sc.close();
            }
        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception");
        }

    }
}
