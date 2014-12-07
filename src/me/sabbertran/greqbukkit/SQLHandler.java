package me.sabbertran.greqbukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLHandler
{

    private GReqBukkit main;
    private Connection connection;

    public SQLHandler(GReqBukkit main)
    {
        this.main = main;
    }

    public Connection getCurrentConnection()
    {
        try
        {
            if (connection == null || connection.isClosed())
            {
                Class.forName("com.mysql.jdbc.Driver");
                String url = "jdbc:mymain.getSql()://" + main.getSql().get(0) + ":" + main.getSql().get(1) + "/" + main.getSql().get(2);
                connection = DriverManager.getConnection(url, main.getSql().get(3), main.getSql().get(4));
            }
        } catch (SQLException | ClassNotFoundException ex)
        {
            Logger.getLogger(SQLHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return connection;
    }
}
