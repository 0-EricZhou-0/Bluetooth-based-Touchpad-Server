import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import java.awt.*;
import java.awt.event.*;

import java.awt.Dimension;


import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

class SwitchException extends Exception {}

class SimpleSPPServer {

    Handler handler;

    private static final String separator = Character.toString((char) 9);

    private static final int CLICK = 1;
    private static final int RIGHT_CLICK = 2;
    private static final int DOUBLE_CLICK = 3;
    private static final int MOVE_CURSOR_RELATIVE = 4;
    private static final int MOVE_CURSOR_ABSOLUTE = 5;
    private static final int SELECT = 6;
    private static final int SCROLL = 7;
    private static final int UNDO = 8;
    private static final int COPY = 9;
    private static final int PASTE = 10;
    private static final int CUT = 11;
    private static final int RETURN_TO_DESKTOP = 12;
    private static final int ENABLE_TASK_MODE = 13;
    private static final int SWITCH_APPLICATION = 14;
    private static final int SWITCH_TAB = 15;
    private static final int INPUT_CHARACTER = 16;

    private static final int CANCEL_LAST_ACTION_FUNCTIONAL = 100;
    private static final int HEARTBEAT_FUNCTIONAL = 101;
    private static final int ACTION_NOT_FOUND_FUNCTIONAL = 102;
    private static final int EXITING_TOUCH_PAD_FUNCTIONAL = 103;

    private void touchPadHandleMessage(String message) throws SwitchException {
        System.out.println(message);
        String[] paramList = message.split(separator);
        switch (Integer.parseInt(paramList[0])) {
            case CLICK:
                handler = new HandleClick(InputEvent.BUTTON1_DOWN_MASK);
                break;
            case RIGHT_CLICK:
                handler = new HandleClick(InputEvent.BUTTON3_DOWN_MASK);
                break;
            case DOUBLE_CLICK:
                handler = new HandleDoubleClick();
                break;
            case MOVE_CURSOR_RELATIVE:
                handler = new HandleMouseMoveRelative(Integer.parseInt(paramList[1]), Integer.parseInt(paramList[2]));
                break;
            case MOVE_CURSOR_ABSOLUTE:
                handler = new HandleMouseMoveAbsolute(Integer.parseInt(paramList[1]), Integer.parseInt(paramList[2]));
                break;
            case SELECT:
                handler = new HandleSelect();
                break;
            case SCROLL:
                handler = new HandleScroll(Integer.parseInt(paramList[1]));
                break;
            case UNDO:
                handler = new HandleUndo();
                break;
            case COPY:
                handler = new HandleCopy();
                break;
            case PASTE:
                handler = new HandlePaste();
                break;
            case CUT:
                handler = new HandleCut();
                break;
            case RETURN_TO_DESKTOP:
                handler = new HandleReturnToDesktop();
                break;
            case ENABLE_TASK_MODE:
                handler = new HandleEnableTaskMode();
                break;
            case SWITCH_APPLICATION:
                handler = new HandleSwitchApplication(Integer.parseInt(paramList[1]));
                break;
            case SWITCH_TAB:
                handler = new HandleSwitchTab(Integer.parseInt(paramList[1]));
                break;
            case INPUT_CHARACTER:
                handler = new HandleInputCharacter(paramList[1]);
                break;



            case CANCEL_LAST_ACTION_FUNCTIONAL:
                if (handler != null) {
                    InputControl.releaseAll();
                }
                break;
            case EXITING_TOUCH_PAD_FUNCTIONAL:
                throw new SwitchException();
            default:
        }
    }

    private void gameHandleMessage(String message) throws SwitchException {
        char type = message.charAt(0);
        switch (type) {
            case 'H':
                break;
            case 'Q':
                InputControl.pressKeyStore('Q');
                break;
            case 'R':
                InputControl.storedKeyRelease('Q');
                break;
            case 'X':
                InputControl.releaseAll();
                throw new SwitchException();
            default:
                new HandleKeyPress(type);
        }

    }

    private static class Handler {
        Handler(int... keycode) {
            if (keycode != null) {
                InputControl.pressKeyStore(keycode);
            }
        }
    }

    private static class HandleKeyPress extends Handler {
        HandleKeyPress(int keyCode) {
            InputControl.releaseAll();
            InputControl.pressKeyStore(keyCode);
        }
    }

    private static class HandleInputCharacter extends Handler {
        HandleInputCharacter(String toInput) {
            if (toInput.equals(Character.toString((char) 8))) {
                InputControl.pressKeyImmediateRelease(KeyEvent.VK_BACK_SPACE);
            } else {
                Bluetooth.clip.setContents(new StringSelection(toInput), null);
                InputControl.pressKeyStore(KeyEvent.VK_CONTROL);
                InputControl.pressKeyImmediateRelease(KeyEvent.VK_V);
                InputControl.releaseAll();
            }
        }
    }

    private static class HandleClick extends Handler {
        HandleClick(int button) {
            InputControl.click(button);
        }
    }

    private static class HandleDoubleClick extends Handler {
        HandleDoubleClick() {
            InputControl.doubleClick();
        }
    }

    private static class HandleScroll extends Handler {
        HandleScroll(int val) {
            InputControl.scroll(val);
        }
    }

    private static class HandleMouseMoveRelative extends Handler {
        HandleMouseMoveRelative(int x, int y) {
            InputControl.moveCursorRelative(x / 2, y / 2);
            new Thread(() -> {
                try {
                    Thread.sleep(7);
                    InputControl.moveCursorRelative(x - x / 2, y - y / 2);
                } catch (Exception ignore) {
                }
            }).start();
        }
    }

    private static class HandleMouseMoveAbsolute extends Handler {
        HandleMouseMoveAbsolute(int x, int y) {
            InputControl.moveCursorAbsolute(x, y);
        }
    }

    private static class HandleSelect extends Handler {
        HandleSelect() {
            InputControl.dragOrRelease();
        }
    }

    private static class HandleReturnToDesktop extends Handler {
        HandleReturnToDesktop() {
            InputControl.pressKeyStore(KeyEvent.VK_WINDOWS);
            InputControl.pressKeyImmediateRelease(KeyEvent.VK_D);
            InputControl.releaseAll();
        }
    }

    private static class HandleEnableTaskMode extends Handler {
        HandleEnableTaskMode() {
            InputControl.pressKeyStore(KeyEvent.VK_WINDOWS);
            InputControl.pressKeyImmediateRelease(KeyEvent.VK_TAB);
            InputControl.releaseAll();
        }
    }

    private static class HandleSwitchApplication extends Handler {
        HandleSwitchApplication(int direction) {
            InputControl.pressKeyStore(KeyEvent.VK_ALT);
            if (direction == MOVE_RIGHT) {
                InputControl.storedKeyRelease(KeyEvent.VK_SHIFT);
            } else {
                InputControl.pressKeyStore(KeyEvent.VK_SHIFT);
            }
            InputControl.pressKeyImmediateRelease(KeyEvent.VK_TAB);
        }
    }

    private static class HandleSwitchTab extends Handler {
        HandleSwitchTab(int direction) {
            InputControl.pressKeyStore(KeyEvent.VK_CONTROL);
            if (direction == MOVE_RIGHT) {
                InputControl.storedKeyRelease(KeyEvent.VK_SHIFT);
            } else {
                InputControl.pressKeyStore(KeyEvent.VK_SHIFT);
            }
            InputControl.pressKeyImmediateRelease(KeyEvent.VK_TAB);
        }
    }

    private static class HandleUndo extends Handler {
        HandleUndo() {
            InputControl.pressKeyStore(KeyEvent.VK_CONTROL);
            InputControl.pressKeyImmediateRelease(KeyEvent.VK_Z);
            InputControl.releaseAll();
        }
    }

    private static class HandleCopy extends Handler {
        HandleCopy() {
            InputControl.pressKeyStore(KeyEvent.VK_CONTROL);
            InputControl.pressKeyImmediateRelease(KeyEvent.VK_C);
            InputControl.releaseAll();
        }
    }

    private static class HandlePaste extends Handler {
        HandlePaste() {
            InputControl.pressKeyStore(KeyEvent.VK_CONTROL);
            InputControl.pressKeyImmediateRelease(KeyEvent.VK_V);
            InputControl.releaseAll();
        }
    }

    private static class HandleCut extends Handler {
        HandleCut() {
            InputControl.pressKeyStore(KeyEvent.VK_CONTROL);
            InputControl.pressKeyImmediateRelease(KeyEvent.VK_X);
            InputControl.releaseAll();
        }
    }

    static final byte TAP = 0b000;                  //0
    static final byte MOVE = 0b001;                 //1
    static final byte LONG_PRESS = 0b010;           //2
    static final byte MOVE_LEFT = 0b011;            //3
    static final byte MOVE_RIGHT = 0b100;           //4
    static final byte MOVE_UP = 0b101;              //5
    static final byte MOVE_DOWN = 0b110;            //6

    private InputStream inStream;
    private OutputStream outStream;
    private BufferedReader inReader;
    private PrintWriter outWriter;
    private StreamConnectionNotifier streamConnNotifier ;
    // start server

    public void startServer() throws IOException {

        //Create a UUID for SPP
        UUID uuid = new UUID("1101", true);
        //Create the servicve url
        String connectionString = "btspp://localhost:" + uuid +";name=Sample SPP Server";

        //open server url
        streamConnNotifier = (StreamConnectionNotifier)Connector.open(connectionString);

        //Wait for client connection
        // System.out.println("\nServer Started. Waiting for clients to connect...");
        StreamConnection connection = streamConnNotifier.acceptAndOpen();

        RemoteDevice dev = RemoteDevice.getRemoteDevice(connection);
        System.out.println("Remote device address: " + dev.getBluetoothAddress());

        //read string from spp client
        inStream = connection.openDataInputStream();
        inReader = new BufferedReader(new InputStreamReader(new DataInputStream(inStream), StandardCharsets.UTF_8));
        outStream = connection.openDataOutputStream();
        outWriter = new PrintWriter(new OutputStreamWriter(new DataOutputStream(outStream), StandardCharsets.UTF_8));
        outWriter.println("CONNECTED");
        outWriter.flush();
        System.out.println("CONNECTED");
        // System.out.println("Remote device name: " + dev.getFriendlyName(true));
        // heartbeat = new Heartbeat(3000, outStream);
        // heartbeat.start();
    }

    public void generalListen() throws IOException {
        while (true) {
            String lineRead = inReader.readLine();
            // System.out.println(lineRead);
            if (lineRead.equals("TOUCH_PAD")) {
                try {
                    touchPadListen();
                } catch (SwitchException e) {
                    System.out.println("TOUCH PAD SESSION END");
                    continue;
                }
            }
            if (lineRead.equals("GAME")) {
                try {
                    gameListen();
                } catch (SwitchException e) {
                    System.out.println("GAME SESSION END");
                    continue;
                }
            }
            if (lineRead.equals("EXIT")) {
                System.out.println("EXIT APPLICATION");
                System.exit(-1);
            }
        }
        /*
        //send response to spp client
        outWriter.write("Response String from SPP Server\r\n");
        outWriter.flush();

        outWriter.close();
        streamConnNotifier.close();
        */
    }

    public void touchPadListen() throws IOException, SwitchException {
        System.out.println("TOUCH PAD SESSION START");
        while (true) {
            String lineRead = inReader.readLine();
            // System.out.println(lineRead);
            touchPadHandleMessage(lineRead);
        }
    }

    public void gameListen() throws IOException, SwitchException {
        System.out.println("GAME SESSION START");
        while (true) {
            String lineRead = inReader.readLine();
            // System.out.println(lineRead);
            gameHandleMessage(lineRead);
        }
    }
}

final class InputControl {
    private static int xPosition;
    private static int yPosition;
    private static int height;
    private static int width;
    private static Robot robot;
    private static ArrayList<Integer> lastKeys;
    private static boolean isDragging = false;
    private InputControl() {}
    static boolean cursorMode = true;
    public static void init() {
        xPosition = MouseInfo.getPointerInfo().getLocation().x;
        yPosition = MouseInfo.getPointerInfo().getLocation().y;
        height = (int) Bluetooth.screenSize.getHeight();
        width = (int) Bluetooth.screenSize.getWidth();
        lastKeys = new ArrayList<>();
        System.out.println(Bluetooth.screenSize);
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void moveCursorRelative(int deltaX, int deltaY) {
        xPosition += deltaX;
        yPosition += deltaY;
        if (xPosition < 0) {
            xPosition = 0;
        }
        if (yPosition < 0) {
            yPosition = 0;
        }
        if (xPosition > width) {
            xPosition = width;
        }
        if (yPosition > height) {
            yPosition = height;
        }
        robot.mouseMove(xPosition, yPosition);
    }

    public static void moveCursorAbsolute(int x, int y) {
        xPosition = (int) (x / 10000.0 * Bluetooth.screenSize.width);
        yPosition = (int) (y / 10000.0 * Bluetooth.screenSize.height);
        robot.mouseMove(xPosition, yPosition);
    }

    public static void click(int button) {
        if (button == InputEvent.BUTTON1_DOWN_MASK && isDragging) {
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            isDragging = false;
        } else {
            robot.mousePress(button);
            robot.mouseRelease(button);
        }
    }

    public static void doubleClick() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    public static void resetDrag() {
        isDragging = false;
    }

    public static void dragOrRelease() {
        if (isDragging) {
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } else {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        }
        isDragging = !isDragging;
    }

    public static void releaseLeftButton() {
        isDragging = false;
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    public static void scroll(int val) {
        robot.mouseWheel(val);
    }

    public static void storedKeyRelease(int keycode) {
        lastKeys.remove(Integer.valueOf(keycode));
        robot.keyRelease(keycode);
    }

    public static void pressKeyStore(int... keycode) {
        for (int key : keycode) {
            lastKeys.add(key);
            robot.keyPress(key);
        }
    }

    public static void releaseAll() {
        for (int key : lastKeys) {
            robot.keyRelease(key);
        }
        lastKeys.clear();
    }

    public static void pressKeyImmediateRelease(int keycode) {
        robot.keyPress(keycode);
        robot.keyRelease(keycode);
    }
}

public class Bluetooth {
    static Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
    static Dimension screenSize;

    public static void main(String[] args) throws IOException {
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        //display local device address and name
        LocalDevice localDevice = LocalDevice.getLocalDevice();
        System.out.println("Name: "+localDevice.getFriendlyName());
        System.err.println("Bluetooth address of this machine: \n"+localDevice.getBluetoothAddress() + "\n========================================");
        // GUI dialog = new GUI();
        InputControl.init();

        SimpleSPPServer SPPServer = new SimpleSPPServer();
        SPPServer.startServer();
        SPPServer.generalListen();

    }
}