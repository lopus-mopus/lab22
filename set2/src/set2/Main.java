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
            /*обновление списка скоростей, и времени ожидания каналов*/
            int refreshTimeout = 3000;
            /*время последнего обновления*/
            long lastUpdate = 0;

            ServerSocketChannel server = ServerSocketChannel.open();
            SocketAddress port = new InetSocketAddress(14204);
            server.socket().bind(port);
            server.configureBlocking(true);

            /*создали селектор для мультиплексированного доступа*/
            Selector selector = Selector.open();
            int operations = SelectionKey.OP_READ;

            /*список для хранения прочитанный битов*/
            Map<SelectionKey, Long> speedMap = new HashMap<>();
            /*буфер для дальнейшего хранения читаемых данных*/
            ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
            lastUpdate = System.currentTimeMillis();

            for (;;)
            {
                /*accept() возвращает SocketChannel, если новый клиент и 0, если это не так*/
                SocketChannel newClient = server.accept();

                /*если нашелся новый клиент*/
                if (newClient != null)
                {
                    server.configureBlocking(false);
                    newClient.configureBlocking(false);
                    System.out.println("New client: " + newClient.socket().getInetAddress());
                    /*добавили новый канал в селектор и вернули его ключ*/
                    SelectionKey newChannel = newClient.register(selector, operations);
                    /*свзяали этот ключ с текущим временем, далее пригодится для расчета скорости*/
                    newChannel.attach(System.currentTimeMillis());
                    /*добавили новый кагал в список, изначально его скрость равна 0*/
                    speedMap.put(newChannel, (long) 0);
                }

                /*если есть клиенты*/
                else if (selector.keys().size() > 0)
                {
                    /*обновляем список ключей, который котовы к тому, чтобы писать, чтобы не ждать долго вводим время таймаута*/
                    selector.select(refreshTimeout);
                    /*создали список всех готовых ключей*/
                    Set<SelectionKey> ready = selector.selectedKeys();
                    /*создали итератор для хождения по этому списку*/
                    Iterator<SelectionKey> readyChannelsIter = ready.iterator();

                    /*пока есть элементы в этом списке*/
                    while (readyChannelsIter.hasNext())
                    {
                        /*получили ключ из списка*/
                        SelectionKey key = readyChannelsIter.next();
                        /*если канал готов к чтению*/
                        if (key.isReadable())
                        {
                            /*получили канал, для которого создавался ключ*/
                            SocketChannel currentSocket = (SocketChannel) key.channel();
                            /*прочитано байт*/
                            long readyBytes = 0;
                            /*получили количество байт, прочитанных до этого*/
                            long summaryBlock = speedMap.get(key);

                            /*читаем байты и записываем их в буфер, в readyBytes записали количество прочитаных байтов*/
                            readyBytes = currentSocket.read(buf);
                            /*очистили буфер*/
                            buf.clear();

                            /*если не прочитали ничего, то закрываем канал*/
                            if (readyBytes <= 0)
                            {
                                key.cancel();
                            }

                            /*в speedMap положили количество прочитанных битов*/
                            speedMap.put(key, summaryBlock + readyBytes * 8);
                        }

                        /*если ключ недоступен, либо отменен*/
                        if (!key.isValid())
                        {
                            /*выводим информацию об удалении клиента*/
                            System.out.println("Remove client: " + ((SocketChannel) key.channel()).socket().getInetAddress());
                            /*повторно отменяем канал, еси он был недоступен, а не отменен*/
                            key.cancel();
                            /*удаляем элемент из speedMap*/
                            speedMap.remove(key);
                        }

                        /*удаляем элемент, с которым сейчас работали*/
                        readyChannelsIter.remove();
                    }
                }
                else
                    /*иначе заблокировали канал*/
                    server.configureBlocking(true);

                /*если прошло время обновления*/
                if (lastUpdate + refreshTimeout < System.currentTimeMillis())
                {
                    /*моздали итератор, для хождения по списку speedMap*/
                    Iterator<Map.Entry<SelectionKey, Long>> it = speedMap.entrySet().iterator();

                    /*пока есть элементы в этом списке*/
                    while (it.hasNext())
                    {
                        /*взяли пару*/
                        Map.Entry<SelectionKey, Long> tempObject = it.next();
                        /*получили канал по ключу*/
                        SocketChannel tempKey = (SocketChannel) tempObject.getKey().channel();
                        /*рассчитали скорость (полученное количесто бит, деленное на время) и вывели его*/
                        System.out.println(tempKey.socket().getInetAddress() + " speed: " + ((tempObject.getValue() * 1000) /
                                ((System.currentTimeMillis() - (Long) tempObject.getKey().attachment()))) + " bit/sec");

                        /*обновили значенеие в 0*/
                        speedMap.put(tempObject.getKey(), (long) 0);
                        /*свзяали с текущим временем*/
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
