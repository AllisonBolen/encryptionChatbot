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

        try {
            System.out.println("What is the server's IP address?");
            String IP = scan.nextLine();
            System.out.println("What is the server's port?");
            int portNum = scan.nextInt();
            SocketChannel sc = SocketChannel.open();
            sc.connect(new InetSocketAddress(IP, portNum));
            System.out.println("Connected to Chat Server");

            // send initial sym key to server for this client
            ByteBuffer buffer = ByteBuffer.wrap(encryptedsecret);
            sc.write(buffer);
            System.out.println("Made and sent symKey to server");

            // create thread for this client
            TcpClientThread t = new TcpClientThread(sc, ct, s);
            t.start();
            while (true) {
                // create random iv params
                //SecureRandom r2 = new SecureRandom();
                byte ivBytes2[] = new byte[16];
                r.nextBytes(ivBytes2);
                IvParameterSpec iv2 = new IvParameterSpec(ivBytes2);

                Console cons = System.console();
                String m = cons.readLine("Enter your message: ");

                if (m.equals("Quit")) {
                    // encrypt message
                    byte ciphertext[] = ct.encrypt(m.getBytes(), s, iv2);
                    //
                    ByteBuffer reffub = ByteBuffer.allocate(ciphertext.length + ivBytes2.length);
                    reffub.put(ivBytes2);
                    reffub.put(ciphertext);
                    reffub.flip();
                    byte total[] = new byte[reffub.remaining()];
                    reffub.get(total);
                    reffub.flip();
                    sc.write(reffub);
                    sc.close();
                    System.exit(0);
                } else {
                    // encrypt the message
                    byte ciphertext[] = ct.encrypt(m.getBytes(), s, iv2);
                    //
                    System.out.println("Cipher: "+ ciphertext + " " + " length: " + ciphertext.length);
                    System.out.println("IvBytes: "+ ivBytes2 + " " + " length: " + ivBytes2.length);
                    ByteBuffer reffub = ByteBuffer.allocate(ciphertext.length + ivBytes2.length);
                    reffub.put(ivBytes2);
                    reffub.put(ciphertext);
                    reffub.flip();
                    byte total[] = new byte[reffub.remaining()];
                    reffub.get(total);
                    reffub.flip();
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
    cryptotest ct;
    SecretKey s;
    TcpClientThread(SocketChannel channel, cryptotest c, SecretKey sk) {
        sc = channel;
        ct = c;
        s = sk;
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
                //////////////////////

                byte[] ivBytesReceived = Arrays.copyOfRange(a, 0, 16);
                IvParameterSpec ivReceived = new IvParameterSpec(ivBytesReceived);

                byte[] A = Arrays.copyOfRange(a, 16, a.length);
                System.out.println("Cipher: "+ A + " " + " length: " + A.length);
                System.out.println("IvBytes: "+ ivBytesReceived + " " + " length: " + ivBytesReceived.length);

                byte[] decryptedplaintext = ct.decrypt(A, s, ivReceived);
                String message = new String(decryptedplaintext);

                ////////////////////////
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
