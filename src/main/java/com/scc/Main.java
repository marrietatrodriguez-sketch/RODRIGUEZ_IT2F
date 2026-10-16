
package com.scc;

import com.scc.dao.UserDao;
import com.scc.ui.MenuScreens;
import java.util.Scanner;

public class Main {

    public static final Scanner sc = new Scanner(System.in);
    public static final UserDao userDao = new UserDao();

    public static void main(String[] args) {

        MenuScreens menu = new MenuScreens();
        menu.showMenu();

    }
}
