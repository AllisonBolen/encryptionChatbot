import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap; // https://stackoverflow.com/questions/2836267/concurrenthashmap-in-java
import java.util.Vector;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import javax.xml.bind.DatatypeConverter;


class tcpechoserver {
    public static void main(String args[]) {
	    cryptotest ct = new cryptotest();
	    ct.setPrivateKey("RSApriv.der");
        Scanner scan = new Scanner(System.in);
        // list to store relevent code for the socket
        Vector<TcpServerThread> threadList = new Vector<TcpServerThread>();
        // map to store all connected client ips and threads
        ConcurrentHashMap<TcpServerThread, SocketChannel> clientMap = new ConcurrentHashMap<TcpServerThread, SocketChannel>();
        Map<TcpServerThread, SocketChannel> map = clientMap;

        try {
            System.out.println("Enter a port for the server to run on: ");
            int port = scan.nextInt();
            ServerSocketChannel c = ServerSocketChannel.open();
            c.bind(new InetSocketAddress(port));

            while (true) {
                SocketChannel sc = c.accept();
                System.out.println("Client Connected: " + sc.getRemoteAddress());

                // get sym key for the thread byt decrypting it by rsa save that key to the thread or make a map of keys to threads
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                sc.read(buffer);
                buffer.flip();
                byte[] a = new byte[buffer.remaining()];
                buffer.get(a);

                // key
                // decyrpt a for the key we need
                byte decryptedsecret[] = ct.RSADecrypt(a);
                SecretKey ds = new SecretKeySpec(decryptedsecret,"AES");

                TcpServerThread t = new TcpServerThread(sc, clientMap, ct, ds);
                threadList.add(t);
                if (!clientMap.containsKey(sc.getRemoteAddress())) {
                    clientMap.putIfAbsent(t, sc);
                }
                // this can be condensed and use the map to loop
                for (Map.Entry<TcpServerThread, SocketChannel> entry : map.entrySet()) {
                    entry.getKey().updateMap(clientMap);
                }
                t.start();
            }
        } catch (IOException e) {
            System.out.println("Issue connecting to port.");
        }
    }
}

class TcpServerThread extends Thread {
    SocketChannel sc;
    SocketChannel tempSc;
    Map<TcpServerThread, SocketChannel> map;
    ConcurrentHashMap<TcpServerThread, SocketChannel> clientMap;
    cryptotest ct;
    SecretKey symKey;
    SecureRandom r = new SecureRandom();

    private boolean running = true;

    TcpServerThread(SocketChannel channel, ConcurrentHashMap<TcpServerThread, SocketChannel> cMap, cryptotest c, SecretKey sk) {
        sc = channel;
        clientMap = cMap;
        map = clientMap;
        ct=c;
        symKey = sk;
    }

    public void run() {
        // main method ?
        try {
            while (running) {
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                sc.read(buffer);
                buffer.flip();
                byte[] a = new byte[buffer.remaining()];
                buffer.get(a);

                ///////////////////// decrypt messgage
                byte[] ivBytesReceived = Arrays.copyOfRange(a, 0, 16);
                IvParameterSpec ivReceived = new IvParameterSpec(ivBytesReceived);
                byte[] A = Arrays.copyOfRange(a, 16, a.length);
                byte[] decryptedplaintext = ct.decrypt(A, this.getSymKey(), ivReceived);
                String message = new String(decryptedplaintext);
                /////////////////////

                System.out.println("Got from client: " + message);
                if (message.equals("Quit")) {
                    sc.close();
                    System.out.println("Client has disconnected");
                    running = false;
                } else if (message.contains("killuser")) {
                    try {
                        Scanner scanner = new Scanner(message);
                        String command = scanner.next();
                        String name = scanner.next();
                        String password = scanner.next();
                        if (password.equals("football") && command.equals("killuser")) {
                            boolean sent = false;
                            // needs to find scoket with name mathc to user input htread other than that logic is good
                            for (Map.Entry<TcpServerThread, SocketChannel> entry : map.entrySet()) {
                                if (Integer.parseInt(name) == entry.getKey().getId()) {
                                    sent = true;
                                    String m10 = "Quit";
                                    entry.getKey().setRunning(false);
                                    SocketChannel sock = entry.getValue();
                                    String m1 = "Admin logged in successfully. Killing user: " + name;
                                    send(sc, m1, this.symKey);
                                    send(sock, m10, entry.getKey().symKey);
                                }
                            }
                            if (!sent) {
                                String m9 = "Incorrect 'Killuser' command: '" + name + " 'isnt connected.";
                                send(sc, m9, this.symKey);
                            }

                        } else if (!command.equals("killuser")) {
                            String m2 = "Incorrect admin command format: use COMMAND INTEGER PASSWORD format";
                            send(sc, m2, this.symKey);
                        } else {
                            String m3 = "Incorrect admin password.";
                            send(sc, m3, this.symKey);
                        }
                    } catch (Exception e) {
                        String m4 = "Incorrect admin command format: use COMMAND INTEGER PASSWORD format";
                        send(sc, m4, this.symKey);
                    }

                } else if (message.contains("list connections")) {
                    send(sc, printMap(), this.symKey);
                } else if (message.contains("broadcast")) { // broad cast command

                    try {
                        Scanner scanner = new Scanner(message);
                        String bd = scanner.next();
                        String data = scanner.next();
                        while (scanner.hasNext()) {
                            data = data + " " + scanner.next();
                        }
                        if (!bd.equals("broadcast")) { // check proper command trigger statment in index one of input if not send error
                            String m7 = "Incorrect 'Broadcast' command format. Use: 'Broadcast MESSAGE'.";
                            send(sc, m7, this.symKey);
                        } else { // broad cast to all scockets in map
                            String info = "From Broadcast: " + data;
                            for (Map.Entry<TcpServerThread, SocketChannel> entry : map.entrySet()) {
                                if(t.isRunning()) {
                                    ByteBuffer buf = ByteBuffer.wrap(info.getBytes());
                                    send(entry.getValue(), info, entry.getKey().symKey);
                                }
                            }
                        }
                    } catch (Exception e) {
                        String m8 = "Incorrect 'Broadcast' command format. Use: 'Broadcast MESSAGE': Your message is blank.";
                        send(sc, m8, this.symKey);
                    }

                } else if (message.contains("sendTo")) {
                    try {
                        Scanner scanner = new Scanner(message);
                        String sendto = scanner.next();
                        String name = scanner.next();
                        String data = scanner.next();
                        while (scanner.hasNext()) {
                            data = data + " " + scanner.next();
                        }
                        //error checking
                        // if name isnt here check that at some point
                        // check proper command trigger statment in index one of input if not send error
                        if (!sendto.equals("sendTo")) {
                            String m7 = "Incorrect 'SendTo' command format. Use: 'SendTo USER MESSAGE'.";
                            send(sc, m7, this.symKey);
                        } else {
                            boolean sent = false;
                            // needs to find scoket with name mathc to user input htread other than that logic is good
                            for (Map.Entry<TcpServerThread, SocketChannel> entry : map.entrySet()) {
                                if (Integer.parseInt(name) == entry.getKey().getId()) {
                                    sent = true;
                                    String m10 = "From: " + this.getId() + " Message: " + data;
                                    SocketChannel sock = entry.getValue();
                                    String m9 = "Sending: '" + data + "' to " + name;
                                    send(sc, m9, this.symKey);
                                    //ByteBuffer buf2 = ByteBuffer.wrap(m10.getBytes());
                                    //sock.write(buf2);
                                    send(sock, m10, entry.getKey().symKey );
                                }
                            }
                            if (!sent) {
                                String m9 = "Incorrect 'SendTo' command: '" + name + " 'isnt connected.";
                                send(sc, m9, this.symKey);
                            }
                        }
                    } catch (Exception e) {
                        String m8 = "Incorrect 'SendTo...' command format. Use 'SendTo USER MESSAGE'. Your message is blank.";
                        send(sc, m8, this.symKey);
                    }

                } else {
                    buffer.rewind();
                    sc.write(buffer);
                }
            }
        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception HERE 3");
        }
    }

    void updateMap(ConcurrentHashMap<TcpServerThread, SocketChannel> cMap) {
        clientMap = cMap;
        map = clientMap;
    }

    String printMap() {
        String data = "Clients Connected";
        try {
            for (Map.Entry<TcpServerThread, SocketChannel> entry : map.entrySet()) {
                TcpServerThread t = entry.getKey();
                if(t.isRunning()) {
                    SocketChannel s = entry.getValue();
                    data = data + "\nClient id: " + t.getId() + ", " + "IPAddress: " + s.getRemoteAddress();
                }
            }
        } catch (Exception e) {
            // print error
            System.out.println("Got an IO Exception HERE 4: "+ e);
        }
        return data;
    }

    public SecretKey getSymKey() {
        return symKey;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    void send(SocketChannel socket, String mes, SecretKey symKey) {
        // encrpyt message before sending
        byte ivbytes[] = new byte[16];
        r.nextBytes(ivbytes);
        IvParameterSpec iv = new IvParameterSpec(ivbytes);
        byte[] enMess = ct.encrypt(mes.getBytes(), symKey, iv);

        //ByteBuffer buf = ByteBuffer.wrap(ivbytes).wrap(enMess);
        ByteBuffer reffub = ByteBuffer.allocate(enMess.length + ivbytes.length);
        reffub.put(ivbytes);
        reffub.put(enMess);
        reffub.flip();
        byte total[] = new byte[reffub.remaining()];
        reffub.get(total);
        reffub.flip();
        try {
            socket.write(reffub);
        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception HERE 5");
        }
    }
}
