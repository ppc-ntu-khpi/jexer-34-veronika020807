package com.mybank.tui;

import jexer.*;
import jexer.event.TMenuEvent;
import jexer.menu.TMenu;

import java.io.File;
import java.util.Scanner;

public class TUIdemo extends TApplication {

    private static final int ABOUT_APP = 2000;
    private static final int CUST_INFO = 2010;

    private final Bank bank = new Bank();

    public static void main(String[] args) throws Exception {
        TUIdemo app = new TUIdemo();
        (new Thread(app)).start();

        // Дочекатися запуску Jexer перед завантаженням файлу
        Thread.sleep(500);
        app.loadCustomersFromFile("src/data/test.dat");
    }

    public TUIdemo() throws Exception {
        super(BackendType.SWING);

        addToolMenu();
        TMenu fileMenu = addMenu("&File");
        fileMenu.addItem(CUST_INFO, "&Customer Info");
        fileMenu.addDefaultItem(TMenu.MID_SHELL);
        fileMenu.addSeparator();
        fileMenu.addDefaultItem(TMenu.MID_EXIT);

        addWindowMenu();

        TMenu helpMenu = addMenu("&Help");
        helpMenu.addItem(ABOUT_APP, "&About...");

        setFocusFollowsMouse(true);
        ShowCustomerDetails();
    }

    @Override
    protected boolean onMenu(TMenuEvent menu) {
        if (menu.getId() == ABOUT_APP) {
            messageBox("About", "TUIdemo App\n(c) 2025").show();
            return true;
        }
        if (menu.getId() == CUST_INFO) {
            ShowCustomerDetails();
            return true;
        }
        return super.onMenu(menu);
    }

    public void loadCustomersFromFile(String filename) {
    try (Scanner sc = new Scanner(new File(filename))) {
        int numCustomers = Integer.parseInt(sc.nextLine().trim());

        for (int i = 0; i < numCustomers; i++) {
            // Пропускаємо порожні рядки
            String nameLine;
            do {
                if (!sc.hasNextLine()) return;
                nameLine = sc.nextLine().trim();
            } while (nameLine.isEmpty());

            String[] nameParts = nameLine.split("\\s+");
            if (nameParts.length < 3) {
                System.err.println("Invalid customer line: " + nameLine);
                continue;
            }

            String firstName = nameParts[0];
            String lastName = nameParts[1];
            int numAccounts = Integer.parseInt(nameParts[2]);

            Customer customer = new Customer(firstName, lastName);

            for (int j = 0; j < numAccounts; j++) {
                String accLine;
                do {
                    if (!sc.hasNextLine()) return;
                    accLine = sc.nextLine().trim();
                } while (accLine.isEmpty());

                String[] accParts = accLine.split("\\s+");
                if (accParts.length < 3) {
                    System.err.println("Invalid account line: " + accLine);
                    continue;
                }

                String type = accParts[0];
                double balance = Double.parseDouble(accParts[1]);

                if (type.equals("S")) {
                    double interestRate = Double.parseDouble(accParts[2]);
                    customer.addAccount(new SavingsAccount(balance, interestRate));
                } else if (type.equals("C")) {
                    double overdraft = Double.parseDouble(accParts[2]);
                    customer.addAccount(new CheckingAccount(balance, overdraft));
                }
            }

            bank.addCustomer(customer);
        }

    } catch (Exception e) {
        System.err.println("Error loading customers: " + e.getMessage());
    }
}


    private void ShowCustomerDetails() {
        TWindow custWin = addWindow("Customer Info", 2, 1, 50, 12, TWindow.NOZOOMBOX);
        custWin.newStatusBar("Enter customer number (0 - " + (bank.getNumberOfCustomers() - 1) + ") and press Show");

        custWin.addLabel("Enter customer number:", 2, 2);
        TField custNo = custWin.addField(26, 2, 3, false);
        TText details = custWin.addText("", 2, 4, 46, 6);

        custWin.addButton("Show", 36, 2, new TAction() {
            @Override
            public void DO() {
                try {
                    int index = Integer.parseInt(custNo.getText());
                    if (index >= 0 && index < bank.getNumberOfCustomers()) {
                        Customer customer = bank.getCustomer(index);
                        Account acc = customer.getAccount(0);

                        String accType = acc instanceof CheckingAccount ? "Checking" : "Savings";
                        String output = "Customer: " + customer.getFirstName() + " " + customer.getLastName() + "\n" +
                                        "Account Type: " + accType + "\n" +
                                        "Balance: $" + String.format("%.2f", acc.getBalance());

                        details.setText(output);
                    } else {
                        messageBox("Error", "No such customer!").show();
                    }
                } catch (Exception e) {
                    messageBox("Error", "Invalid input!").show();
                }
            }
        });
    }
}

// ==== Класи моделі банку ====

class Bank {
    private Customer[] customers = new Customer[50];
    private int numberOfCustomers = 0;

    public void addCustomer(Customer customer) {
        customers[numberOfCustomers++] = customer;
    }

    public Customer getCustomer(int index) {
        return customers[index];
    }

    public int getNumberOfCustomers() {
        return numberOfCustomers;
    }
}

class Customer {
    private String firstName;
    private String lastName;
    private Account[] accounts = new Account[10];
    private int numberOfAccounts = 0;

    public Customer(String f, String l) {
        this.firstName = f;
        this.lastName = l;
    }

    public void addAccount(Account acc) {
        accounts[numberOfAccounts++] = acc;
    }

    public Account getAccount(int index) {
        return accounts[index];
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
}

abstract class Account {
    protected double balance;

    public Account(double balance) {
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }
}

class CheckingAccount extends Account {
    private double overdraftLimit;

    public CheckingAccount(double balance, double overdraftLimit) {
        super(balance);
        this.overdraftLimit = overdraftLimit;
    }
}

class SavingsAccount extends Account {
    private double interestRate;

    public SavingsAccount(double balance, double interestRate) {
        super(balance);
        this.interestRate = interestRate;
    }
}
