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


class tcpechoserver {
    public static void main(String args[]) {
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
                TcpServerThread t = new TcpServerThread(sc, clientMap);
                threadList.add(t);
                if (!clientMap.containsKey(sc.getRemoteAddress())) {
                    clientMap.putIfAbsent(t, sc);
                }
                System.out.println(clientMap.toString());
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

    private boolean running = true;

    TcpServerThread(SocketChannel channel, ConcurrentHashMap<TcpServerThread, SocketChannel> cMap) {
        sc = channel;
        clientMap = cMap;
        map = clientMap;
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
                String message = new String(a);
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
                                    send(sc, m1);
                                    ByteBuffer buf2 = ByteBuffer.wrap(m10.getBytes());
                                    sock.write(buf2);
                                }
                            }
                            if (!sent) {
                                String m9 = "Incorrect 'Killuser' command: '" + name + " 'isnt connected.";
                                send(sc, m9);
                            }

                        } else if (!command.equals("killuser")) {
                            String m2 = "Incorrect admin command format: use COMMAND INTEGER PASSWORD format";
                            send(sc, m2);
                        } else {
                            String m3 = "Incorrect admin password.";
                            send(sc, m3);
                        }
                    } catch (Exception e) {
                        String m4 = "Incorrect admin command format: use COMMAND INTEGER PASSWORD format";
                        send(sc, m4);
                    }

                } else if (message.contains("list connections")) {
                    send(sc, printMap());
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
                            send(sc, m7);
                        } else { // broad cast to all scockets in map
                            String info = "From Broadcast: " + data;
                            for (Map.Entry<TcpServerThread, SocketChannel> entry : map.entrySet()) {
                                ByteBuffer buf = ByteBuffer.wrap(info.getBytes());
                                send(entry.getValue(), info);
                            }
                        }
                    } catch (Exception e) {
                        String m8 = "Incorrect 'Broadcast' command format. Use: 'Broadcast MESSAGE': Your message is blank.";
                        send(sc, m8);
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
                            send(sc, m7);
                        } else {
                            boolean sent = false;
                            // needs to find scoket with name mathc to user input htread other than that logic is good
                            for (Map.Entry<TcpServerThread, SocketChannel> entry : map.entrySet()) {
                                if (Integer.parseInt(name) == entry.getKey().getId()) {
                                    sent = true;
                                    String m10 = "From: " + this.getId() + " Message: " + data;
                                    SocketChannel sock = entry.getValue();
                                    String m9 = "Sending: '" + data + "' to " + name;
                                    send(sc, m9);
                                    ByteBuffer buf2 = ByteBuffer.wrap(m10.getBytes());
                                    sock.write(buf2);
                                }
                            }
                            if (!sent) {
                                String m9 = "Incorrect 'SendTo' command: '" + name + " 'isnt connected.";
                                send(sc, m9);
                            }
                        }
                    } catch (Exception e) {
                        String m8 = "Incorrect 'SendTo...' command format. Use 'SendTo USER MESSAGE'. Your message is blank.";
                        send(sc, m8);
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
                    System.out.println("here 2: " + t.getId());

                    SocketChannel s = entry.getValue();
                    System.out.println("here 3");

                    data = data + "\nClient id: " + t.getId() + ", " + "IPAddress: " + s.getRemoteAddress();
                    System.out.println("here 4");
                }
            }
        } catch (Exception e) {
            // print error
            System.out.println("Got an IO Exception HERE 4: "+ e);
        }
        return data;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    void send(SocketChannel socket, String mes) {
        ByteBuffer buf = ByteBuffer.wrap(mes.getBytes());
        try {
            socket.write(buf);
        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception HERE 5");
        }
    }
}
