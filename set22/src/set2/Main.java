package set2;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2)
            throw new RuntimeException("Need more arguments!");

        try {
            byte[] randomData = new byte[99999];
            ByteBuffer buf = ByteBuffer.wrap(randomData);
            Socket socket = new Socket(args[0], Integer.parseInt(args[1]));
            SocketChannel channel = SocketChannel.open(socket.getRemoteSocketAddress());
            channel.configureBlocking(true);
            System.out.println("Connection state: " + channel.isConnected());
            System.out.println("Transferring data...");

            for (;;) {
            	buf = ByteBuffer.allocate(1024*1024*8);
                channel.write(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}















































/*   args[1]="14224";
args[0]="localhost";
if (args.length < 2)
    throw new RuntimeException("Need more arguments!");*/