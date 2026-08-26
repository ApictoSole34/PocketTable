package com.fizzycoyote.pockettable.network.common;

import android.util.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DiscoveryService {
    private static final int DISCOVERY_PORT = 8889;
    private static final String TAG = "DiscoveryService";
    private static boolean broadcasting = false;
    private static Thread broadcastThread;

    public interface DiscoveryListener {
        void onHostFound(String ip, String roomCode, String gameType);
        void onHostNotFound();
    }

    public static void broadcastHost(String roomCode, String hostIp, String gameType) {
        if (broadcasting) return;
        broadcasting = true;

        broadcastThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                String message = "POCKETTABLE_HOST:" + roomCode + ":" + hostIp + ":" + gameType;
                DatagramPacket packet = new DatagramPacket(
                        message.getBytes(),
                        message.length(),
                        InetAddress.getByName("255.255.255.255"),
                        DISCOVERY_PORT
                );
                while (broadcasting) {
                    socket.send(packet);
                    Log.d(TAG, "Broadcast: " + message);
                    Thread.sleep(2000);
                }
            } catch (Exception e) {
                Log.e(TAG, "Broadcast error: " + e.getMessage());
            }
        });
        broadcastThread.start();
    }

    public static void stopBroadcast() {
        broadcasting = false;
        if (broadcastThread != null) {
            broadcastThread.interrupt();
        }
    }

    public static void discoverHost(String targetRoomCode, DiscoveryListener listener) {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
                socket.setSoTimeout(5000);
                byte[] buffer = new byte[1024];
                long startTime = System.currentTimeMillis();
                long timeout = 10000;

                while (System.currentTimeMillis() - startTime < timeout) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(packet);
                        String data = new String(packet.getData(), 0, packet.getLength());
                        if (data.startsWith("POCKETTABLE_HOST:")) {
                            String[] parts = data.split(":");
                            if (parts.length >= 4 && parts[1].equals(targetRoomCode)) {
                                String ip = parts[2];
                                String gameType = parts[3];
                                listener.onHostFound(ip, targetRoomCode, gameType);
                                return;
                            }
                        }
                    } catch (java.net.SocketTimeoutException e) {}
                }
                listener.onHostNotFound();
            } catch (Exception e) {
                Log.e(TAG, "Discover error: " + e.getMessage());
                listener.onHostNotFound();
            }
        }).start();
    }
}