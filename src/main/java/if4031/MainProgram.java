package if4031;

import java.util.Scanner;

public class MainProgram {
    private static void printUsage() {
        System.out.println("Commands:");
        System.out.println("stack push <object>: add object to stack");
        System.out.println("stack pop: remove object from stack");
        System.out.println("stack top: peek the top element of stack");
        System.out.println("set add <element>: add object to a set");
        System.out.println("set remove <element>: remove object from a set");
        System.out.println("set contains <element>: check whether element is in set");
        System.out.println("exit: exit program");
    }

    public static void main(String[] args) throws Exception {
        ReplStack<String> stringStack = new ReplStack<>(String.class);
        ReplSet<String> stringSet = new ReplSet<>(String.class);
        stringStack.start();
        stringSet.start();

        System.out.println("This is a driver program to test ReplStack and ReplSet Implementation");
        printUsage();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String command0 = scanner.next();
            boolean isCommandValid = true;

            if (command0.equals("stack")) {
                String command1 = scanner.next();
                if (command1.equals("push")) {
                    String command2 = scanner.next();
                    stringStack.push(command2);
                    System.out.println(command2 + " pushed to stack.");

                } else if (command1.equals("pop")) {
                    if (stringStack.size() > 0) {
                        String result = stringStack.pop();
                        System.out.println(result + " popped from stack.");
                    } else {
                        System.out.println("stack is empty");
                    }

                } else if (command1.equals("top")) {
                    if (stringStack.size() > 0) {
                        String result = stringStack.top();
                        System.out.println(result);
                    } else {
                        System.out.println("stack is empty");
                    }

                } else {
                    isCommandValid = false;
                }

            } else if (command0.equals("set")) {
                String command1 = scanner.next();
                String command2 = scanner.next();
                if (command1.equals("add")) {
                    boolean result = stringSet.add(command2);
                    System.out.println(command2 + (result ? " added" : " already exist"));

                } else if (command1.equals("remove")) {
                    boolean result = stringSet.remove(command2);
                    System.out.println(command2 + (result ? " removed" : " doesn't exist"));

                } else if (command1.equals("contains")) {
                    boolean result = stringSet.contains(command2);
                    System.out.println("" + result);

                } else {
                    isCommandValid = false;
                }

            } else if (command0.equals("exit")) {
                break;

            } else {
                isCommandValid = false;
            }

            if (!isCommandValid) {
                printUsage();
                System.out.println("Command not recognized");
            }
        }

        stringStack.stop();
        stringSet.stop();
    }
}
