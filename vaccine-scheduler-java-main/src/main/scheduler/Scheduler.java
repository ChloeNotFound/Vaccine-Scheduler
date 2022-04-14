package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        //Extra credit
        if (password.length() < 8) {
            System.out.println("A strong password has to be at least 8 characters.");
            System.out.println("Please try again!");
            return;
        }
        int lower = 0;
        int upper = 0;
        int number = 0;
        int special = 0;
        for (int i = 0; i < password.length(); i++) {
            //lowercase
            if (password.charAt(i) >= 97 && password.charAt(i) <= 122 ) {
                lower++;
            }
            //uppercase
            if (password.charAt(i) >= 65 && password.charAt(i) <= 90 ) {
                upper++;
            }
            //number
            if (password.charAt(i) >= 48 && password.charAt(i) <= 57 ) {
                number++;
            }
            //special characters
            if (password.charAt(i) == 33 || password.charAt(i) == 42
                    ||password.charAt(i) == 47 || password.charAt(i) == 95) {
                special++;
            }
        }

        if(lower == 0 ){
            System.out.println("A strong password should include lowercase letter.");
            System.out.println("Please try again!");
            return;
        }else if(upper == 0){
            System.out.println("A strong password should include uppercase letter.");
            System.out.println("Please try again!");
            return;
        }else if(number == 0){
            System.out.println("A strong password should include a number.");
            System.out.println("Please try again!");
            return;
        }else if(special == 0){
            System.out.println("A strong password has to included of at least one special character, from “!”, “@”, “#”, “?”.");
            System.out.println("Please try again!");
            return;
        }



        //check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the Patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }

        if (password.length() < 8) {
            System.out.println("A strong password has to be at least 8 characters.");
            System.out.println("Please try again!");
            return;
        }
        int lower = 0;
        int upper = 0;
        int number = 0;
        int special = 0;
        for (int i = 0; i < password.length(); i++) {
            if (password.charAt(i) >= 97 && password.charAt(i) <= 122 ) {//lowercase
                lower++;
            }
            if (password.charAt(i) >= 65 && password.charAt(i) <= 90 ) {//uppercase
                upper++;
            }
            if (password.charAt(i) >= 48 && password.charAt(i) <= 57 ) {//number
                number++;
            }
            if (password.charAt(i) == 33 || password.charAt(i) == 42
                    ||password.charAt(i) == 47 || password.charAt(i) == 95) {//special characters
                special++;
            }
        }

        if(lower == 0 ){
            System.out.println("A strong password should include lowercase letter.");
            System.out.println("Please try again!");
            return;
        }else if(upper == 0){
            System.out.println("A strong password should include uppercase letter.");
            System.out.println("Please try again!");
            return;
        }else if(number == 0){
            System.out.println("A strong password should include a number.");
            System.out.println("Please try again!");
            return;
        }else if(special == 0){
            System.out.println("A strong password has to included of at least one special character, from “!”, “@”, “#”, “?”.");
            System.out.println("Please try again!");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patient WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) throws SQLException {
        // TODO: Part 2
        if (tokens.length != 2) {
            System.out.println("Invalid input format, please try again!");
            return;
        }
        try {
            LocalDate today = LocalDate.now();
            LocalDate date = LocalDate.parse(tokens[1]);
            if (date.isBefore(today)) {
                System.out.println("Invalid input date, please try again!");
                return;
            }
        } catch (DateTimeParseException e) {
            System.out.println("Invalid input date, please try again!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getSchedule = "SELECT A.Username, V.Name, V.Doses " +
                "FROM Availabilities AS A, Vaccines AS V, Caregivers As C " +
                "WHERE A.Time = ?";

        ConnectionManager cm2 = new ConnectionManager();
        Connection con2 = cm2.createConnection();


        try {
            PreparedStatement statement = con.prepareStatement(getSchedule);
            statement.setString(1, tokens[1]);
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date sqlDate = sdf1.parse(tokens[1]);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                System.out.println("Care_username : " + rs.getString(0));
                System.out.println("Vaccine name :" + rs.getString(1));
                System.out.println("# of doses :" + rs.getInt(2));
            } else {
                System.out.println("No such result");
            }
            con.close();


        } catch (SQLException | ParseException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();

        }
    }

    private static void reserve(String[] tokens) throws SQLException {
        // TODO: Part 2
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        try {
            LocalDate today = LocalDate.now();
            LocalDate date = LocalDate.parse(tokens[1]);
            if (date.isBefore(today)) {
                System.out.println("Invalid input date, please try again!");
                return;
            }
        } catch (DateTimeParseException e) {
            System.out.println("Invalid input date, please try again!");
            return;
        }
        String date = tokens[1];
        String vaccineName = tokens[2];
        Random ran = new Random();
        int appID = ran.nextInt(99999999);
        String patientName = currentPatient.getUsername();

        // Extract doses from Vaccine
        ConnectionManager cm4 = new ConnectionManager();
        Connection con4 = cm4.createConnection();
        int dosesNum = 0;

        String getDoses = "SELECT V.Doses " +
                "FROM Vaccines AS V " +
                "WHERE V.Name = ?";


        try {
            PreparedStatement statement = con4.prepareStatement(getDoses);
            statement.setString(1, vaccineName);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                dosesNum = rs.getInt(1);
            }
            con4.close();


        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm4.closeConnection();
        }

        if (dosesNum == 0) {
            System.out.println("No enough doses now.");
            return;
        }
        // get care_name
        ArrayList<String> care_nameList = new ArrayList<>();
        ConnectionManager cm1 = new ConnectionManager();
        Connection con1 = cm1.createConnection();

        String getSchedule = "SELECT A.Username " +
                "FROM Availabilities AS A " +
                "WHERE A.Time = ?";



        try {
            PreparedStatement statement = con1.prepareStatement(getSchedule);
            statement.setString(1, date);

            Statement stmt = con1.createStatement();
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date sqlDate = sdf1.parse(tokens[1]);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                care_nameList.add(rs.getString(1));
            }
            con1.close();


        } catch (SQLException | ParseException e) {
            throw new SQLException();
        } finally {
            cm1.closeConnection();

        }

        int randomCare_name = ran.nextInt(care_nameList.size());
        String care_name = care_nameList.get(randomCare_name);

        // add value into appointment table
        ConnectionManager cm2 = new ConnectionManager();
        Connection con2 = cm2.createConnection();

        String addApp = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con2.prepareStatement(addApp);
            statement.setInt(1, appID);
            statement.setString(2, vaccineName);
            statement.setString(3, date);
            statement.setString(4, patientName);
            statement.setString(5, care_name);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm2.closeConnection();
        }

        // add value into Availabilities table
        ConnectionManager cm3 = new ConnectionManager();
        Connection con3 = cm3.createConnection();

        String addAvail = "INSERT INTO Availabilities VALUES (?, ?)";
        try {
            PreparedStatement statement = con3.prepareStatement(addAvail);
            statement.setString(1, date);
            statement.setString(2, care_name);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm3.closeConnection();
        }

        // add value into vaccine table
        ConnectionManager cm5 = new ConnectionManager();
        Connection con5 = cm5.createConnection();

        String addVaccine = "UPDATE Vaccines SET Doses = ?";
        try {
            PreparedStatement statement = con5.prepareStatement(addVaccine);
            statement.setInt(1, dosesNum - 1);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm5.closeConnection();
        }


        // output
        System.out.println("Assigned caregiver: " + care_name);
        System.out.println("Appointment ID: " + appID);
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }


    }

    private static void cancel(String[] tokens) throws SQLException {
        // TODO: Extra credit
        if (tokens.length != 2) {
            System.out.println("Invalid input");
            return;
        }
        int appID = Integer.parseInt(tokens[1]);

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String cancelApp = "DELETE FROM Appointments WHERE id = ?";
        try {
            PreparedStatement statement = con.prepareStatement(cancelApp);
            statement.setInt(1, appID);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }


    }

    private static void addDoses(String[] tokens) {
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else  {
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) throws SQLException {
        // TODO: Part 2
        if (currentPatient != null) {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String patientShowApp = "SELECT id, Vaccine_name, Time, Caregiver_name FROM Appointments";
            try {
                PreparedStatement statement = con.prepareStatement(patientShowApp);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    System.out.println("Appointment ID : " + rs.getInt(0));
                    System.out.println("Vaccine name :" + rs.getString(1));
                    System.out.println("Appointment date :" + rs.getString(2));
                    System.out.println("Caregiver name :" + rs.getString(3));
                } else {
                    System.out.println("No such result");
                }
                con.close();
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }

        } else if (currentCaregiver != null) {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String caregiverShowApp = "SELECT id, Vaccine_name, Time, Patient_name " +
                    "FROM Appointments";
            try {
                PreparedStatement statement = con.prepareStatement(caregiverShowApp);
                ResultSet rs = statement.executeQuery();

                if (rs.next()) {
                    System.out.println("Appointment ID : " + rs.getInt(0));
                    System.out.println("Vaccine name :" + rs.getString(1));
                    System.out.println("Appointment date :" + rs.getString(2));
                    System.out.println("Patient name :" + rs.getString(3));
                } else {
                    System.out.println("No such result");
                }
                con.close();
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        } else {
            System.out.println("Please log in first");
            return;
        }

    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        currentCaregiver = null;
        currentPatient = null;
        System.out.println("---log out successfully---");
    }
}