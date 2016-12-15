package set2;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class Main {

    public static final int BUFFER_SIZE = 99999;

    public static void main(String[] args) {
        try
        {
            /*���������� ������ ���������, � ������� �������� �������*/
            int refreshTimeout = 3000;
            /*����� ���������� ����������*/
            long lastUpdate = 0;

            ServerSocketChannel server = ServerSocketChannel.open();
            SocketAddress port = new InetSocketAddress(14204);
            server.socket().bind(port);
            server.configureBlocking(true);

            /*������� �������� ��� ��������������������� �������*/
            Selector selector = Selector.open();
            int operations = SelectionKey.OP_READ;

            /*������ ��� �������� ����������� �����*/
            Map<SelectionKey, Long> speedMap = new HashMap<>();
            /*����� ��� ����������� �������� �������� ������*/
            ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
            lastUpdate = System.currentTimeMillis();

            for (;;)
            {
                /*accept() ���������� SocketChannel, ���� ����� ������ � 0, ���� ��� �� ���*/
                SocketChannel newClient = server.accept();

                /*���� ������� ����� ������*/
                if (newClient != null)
                {
                    server.configureBlocking(false);
                    newClient.configureBlocking(false);
                    System.out.println("New client: " + newClient.socket().getInetAddress());
                    /*�������� ����� ����� � �������� � ������� ��� ����*/
                    SelectionKey newChannel = newClient.register(selector, operations);
                    /*������� ���� ���� � ������� ��������, ����� ���������� ��� ������� ��������*/
                    newChannel.attach(System.currentTimeMillis());
                    /*�������� ����� ����� � ������, ���������� ��� ������� ����� 0*/
                    speedMap.put(newChannel, (long) 0);
                }

                /*���� ���� �������*/
                else if (selector.keys().size() > 0)
                {
                    /*��������� ������ ������, ������� ������ � ����, ����� ������, ����� �� ����� ����� ������ ����� ��������*/
                    selector.select(refreshTimeout);
                    /*������� ������ ���� ������� ������*/
                    Set<SelectionKey> ready = selector.selectedKeys();
                    /*������� �������� ��� �������� �� ����� ������*/
                    Iterator<SelectionKey> readyChannelsIter = ready.iterator();

                    /*���� ���� �������� � ���� ������*/
                    while (readyChannelsIter.hasNext())
                    {
                        /*�������� ���� �� ������*/
                        SelectionKey key = readyChannelsIter.next();
                        /*���� ����� ����� � ������*/
                        if (key.isReadable())
                        {
                            /*�������� �����, ��� �������� ���������� ����*/
                            SocketChannel currentSocket = (SocketChannel) key.channel();
                            /*��������� ����*/
                            long readyBytes = 0;
                            /*�������� ���������� ����, ����������� �� �����*/
                            long summaryBlock = speedMap.get(key);

                            /*������ ����� � ���������� �� � �����, � readyBytes �������� ���������� ���������� ������*/
                            readyBytes = currentSocket.read(buf);
                            /*�������� �����*/
                            buf.clear();

                            /*���� �� ��������� ������, �� ��������� �����*/
                            if (readyBytes <= 0)
                            {
                                key.cancel();
                            }

                            /*� speedMap �������� ���������� ����������� �����*/
                            speedMap.put(key, summaryBlock + readyBytes * 8);
                        }

                        /*���� ���� ����������, ���� �������*/
                        if (!key.isValid())
                        {
                            /*������� ���������� �� �������� �������*/
                            System.out.println("Remove client: " + ((SocketChannel) key.channel()).socket().getInetAddress());
                            /*�������� �������� �����, ��� �� ��� ����������, � �� �������*/
                            key.cancel();
                            /*������� ������� �� speedMap*/
                            speedMap.remove(key);
                        }

                        /*������� �������, � ������� ������ ��������*/
                        readyChannelsIter.remove();
                    }
                }
                else
                    /*����� ������������� �����*/
                    server.configureBlocking(true);

                /*���� ������ ����� ����������*/
                if (lastUpdate + refreshTimeout < System.currentTimeMillis())
                {
                    /*������� ��������, ��� �������� �� ������ speedMap*/
                    Iterator<Map.Entry<SelectionKey, Long>> it = speedMap.entrySet().iterator();

                    /*���� ���� �������� � ���� ������*/
                    while (it.hasNext())
                    {
                        /*����� ����*/
                        Map.Entry<SelectionKey, Long> tempObject = it.next();
                        /*�������� ����� �� �����*/
                        SocketChannel tempKey = (SocketChannel) tempObject.getKey().channel();
                        /*���������� �������� (���������� ��������� ���, �������� �� �����) � ������ ���*/
                        System.out.println(tempKey.socket().getInetAddress() + " speed: " + ((tempObject.getValue() * 1000) /
                                ((System.currentTimeMillis() - (Long) tempObject.getKey().attachment()))) + " bit/sec");

                        /*�������� ��������� � 0*/
                        speedMap.put(tempObject.getKey(), (long) 0);
                        /*������� � ������� ��������*/
                        tempObject.getKey().attach(System.currentTimeMillis());
                    }

                    lastUpdate = System.currentTimeMillis();
                }

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
