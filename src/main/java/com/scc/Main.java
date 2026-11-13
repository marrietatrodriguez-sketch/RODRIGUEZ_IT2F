package com.scc;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        DatabaseInitializer.initializeDatabase();

        try (Scanner sc = new Scanner(System.in)) {
            Authentication auth = new Authentication(
                    new UserDao(),
                    new ActivityLogDao(),
                    sc
            );

            auth.start();
        }
    }
}