package com.builder.processes;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;

import static com.builder.utils.Constants.*;

@ManagedBean
@SessionScoped
public class Login {

    private String username;
    private String password;
    private static String userMessage = "";


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void login() throws IOException, SQLException, ClassNotFoundException, ParseException {
        Connection connection = databaseConnection();
        if (isValidUser(connection)) {
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                    .getExternalContext().getResponse();
            response.sendRedirect(WEB_PREFIX_URL + "newFile" + XHTML_SUFFIX);
        } else {
            userMessage = "The user name and password doesn't exist, please try again";
        }
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void createNewUser() throws IOException, SQLException, ClassNotFoundException, ParseException {
        if (username == null || username.length() == 0 || password == null || password.length() == 0) {
            userMessage = "Please fill your user name and your password";
        } else {
            Connection connection = databaseConnection();
            if (!isUserAlreadyExist(connection)) {
                createNewUser(connection);
                HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                        .getExternalContext().getResponse();
                response.sendRedirect(WEB_PREFIX_URL + "newFile" + XHTML_SUFFIX);
            } else {
                userMessage = "The user name is already exist, please peek another one";
            }
        }

    }

    private void createNewUser(Connection connection) throws SQLException {
        String sql = "INSERT INTO users(userName,password) VALUES('" + username + "','" + password + "');";
        Statement myStmt = connection.createStatement();
        myStmt.executeUpdate(sql);
    }

    private boolean isUserAlreadyExist(Connection connection) throws SQLException {
        String sql = BASIC_SELECT + username + "';";
        Statement myStmt = connection.createStatement();
        ResultSet myRs = myStmt.executeQuery(sql);
        boolean exist = false;
        while (myRs.next()) {
            exist = true;
        }
        return exist;
    }

    private boolean isValidUser(Connection connection) throws SQLException {
        String sql = BASIC_SELECT + username + "';";
        Statement myStmt = connection.createStatement();
        ResultSet myRs = myStmt.executeQuery(sql);
        boolean isValid = false;
        while (myRs.next()) {
            if (this.password.equals(myRs.getString("password"))) {
                isValid = true;
            }
        }
        return isValid;
    }

    private Connection databaseConnection() throws ClassNotFoundException, SQLException, IOException, ParseException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Configuration configuration = new Configuration().invoke();
        return DriverManager.getConnection(JDBC_URL, configuration.getUser(), configuration.getPassword());
    }

    private static class Configuration {
        private String user;
        private String password;

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public Configuration invoke() throws IOException, ParseException {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.json");
            if (inputStream == null){
                throw new NullPointerException("There is no config file for database");
            }
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject)jsonParser.parse(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            user = (String) jsonObject.get("username");
            password = (String) jsonObject.get("password");
            return this;
        }
    }
}


