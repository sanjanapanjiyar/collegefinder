import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.text.DecimalFormat;


/**
 * This is a college finder that takes a user's interested major, their user location and how far
 * then want to go. It takes user input of location and interest and returns
 * the list of colleges which provide that major and distance to that college
 */
public class CollegeFinder {

  private Map<Integer, String> loadMajors() {
    Map<Integer, String> majors = new HashMap<>();
    String line;
    try (BufferedReader br = new BufferedReader(new FileReader("src/majors.csv"))) {
      while ((line = br.readLine()) != null) {
        if (line.startsWith("#"))
          continue;
        String[] cols = line.split(",");
        majors.put(Integer.parseInt(cols[0].trim()), cols[1].trim());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return majors;
  }

  /**
   * Reads the CSV files with college name and location in form of
   * longitude and latitude.
   */
  private List<College> loadColleges() {
    String line;
    List<College> colleges = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader("src/college-coordinates.csv"))) {
      while ((line = br.readLine()) != null) {
        // ignore the header row,
        if (line.startsWith("#"))
          continue;
        String[] cols = line.split(",");
        colleges.add(new College(cols[0].trim(), cols[1].trim(), Double.parseDouble(cols[2].trim()), Double.parseDouble(cols[3].trim())));
      }//i would actually create a super large string and then split it up into its comps
    } catch (IOException e) {
      e.printStackTrace();
    }

    Map<String, List<Integer>> collegeMajors = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader("src/college-majors.csv"))) {
      while ((line = br.readLine()) != null) {
        // ignore the header row,
        if (line.startsWith("#"))
          continue;
        String[] cols = line.split(",");
        String[] majors = Arrays.copyOfRange(cols, 1, cols.length);
        List<Integer> majorIds =
            Arrays
                .stream(majors)
                .map(String::trim)
                .map(Integer::valueOf)
                .collect(Collectors.toList());
        collegeMajors.put(cols[0], majorIds);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    for (College college : colleges) {
      if (collegeMajors.containsKey(college.name)) {
        college.setMajors(collegeMajors.get(college.name));
      }
    }
    return colleges;
  }

  private User getUserInput(Scanner scanner) {
    double[] coordinates = getUserLocation(scanner);
    int interestedMajor = getUserInterestedMajor(scanner);
    return new User(coordinates[0], coordinates[1], interestedMajor);
  }

  private int getUserInterestedMajor(Scanner scanner) {
    Map<Integer, String> majors = loadMajors();
    System.out.println("Please see the ID and Name of available majors, and" +
        "pick the id of the major you are interested in.");
    for (Map.Entry<Integer, String> major : majors.entrySet()) {
      System.out.println(major.getKey() + ": " + major.getValue());
    }
    return scanner.nextInt();
  }

  private double[] getUserLocation(Scanner scanner) {
    System.out.println(
        "Please provide you geo location\n" +
            "You can try https://www.gps-coordinates.net/ or any other " +
            "GPS locator service, something like 40.5978128,-74.3832758");
    String[] coordinates = scanner.nextLine().split(",");
    return new double[]{Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1])};
  }

  public static void main(String[] args) {
    System.out.println("Welcome to College Finder");
    Scanner scanner = new Scanner(System.in);
    CollegeFinder finder = new CollegeFinder();
    User user = finder.getUserInput(scanner);
    List<College> colleges = finder.loadColleges();
    Map<Integer, String> majors = finder.loadMajors();

    List<College> offeringCollege = new ArrayList<>();
    for (College college : colleges) {
      if (college.majors.contains(user.interestedMajor)) {
        offeringCollege.add(college);
      }
    }

//    List<College> offeringCollege =
//        colleges.stream()
//            .filter(college -> college.majors.contains(user.interestedMajor))
//            .collect(Collectors.toList());

    DistanceCalculator calculator = new DistanceCalculator(user.latitude, user.longitude);
    for (College college : offeringCollege) {
      college.setDistance(calculator.getDistance(college.latitude, college.longitude));
    }
//    offeringCollege.forEach(college ->
//        college.setDistance(calculator.getDistance(college.longitude, college.longitude))
//    );
    offeringCollege.sort(Comparator.comparingDouble(college -> college.distance));
    printResult(offeringCollege, majors, user.getInterestedMajor());
  }

  private static void printResult(List<College> offeringCollege, Map<Integer, String> majors, int interestedMajor) {
    DecimalFormat df = new DecimalFormat("#,###.##");
    System.out.println("Hello! These are some possible colleges you may be interested in" +
        " which provide your major: "+interestedMajor+"."+majors.get(interestedMajor)+" from closest to farthest in distance");
    //College.forEach(element -> System.out.println(element));
    for (College college : offeringCollege) {
      System.out.println(college.name + ", " + college.state + " is located " + df.format(college.distance * 0.62137) + " miles away from you!");
      System.out.print("It's specializes in:\n" + college.majors.stream().map(m -> "* " + majors.get(m)).collect(Collectors.joining("\n")) + "\n\n");
    }
  }
}

class User {
  double longitude;
  double latitude;
  int interestedMajor;

  public User(double latitude, double longitude, int interestedMajor) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.interestedMajor = interestedMajor;
  }

  public int getInterestedMajor() {
    return interestedMajor;
  }
}

class College {
  String name;
  String state;
  double longitude;
  double latitude;
  List<Integer> majors = new ArrayList<>();
  double distance;

  public College(String name, String state,  double longitude, double latitude) {
    this.name = name;
    this.state = state;
    this.longitude = longitude;
    this.latitude = latitude;
  }

  public void setMajors(List<Integer> majors) {
    this.majors = majors;
  }

  public void setDistance(double distance) {
    this.distance = distance;
  }

  @Override
  public String toString() {
    return "College{" +
        "name='" + name + '\'' +
        ", state='" + state + '\'' +
        ", longitude=" + longitude +
        ", latitude=" + latitude +
        ", distance=" + distance +
        ", majors=" + majors +
        '}';
  }
}


class DistanceCalculator {
  double longitude;
  double latitude;

  public DistanceCalculator(double latitude, double longitude) {
    this.longitude = longitude;
    this.latitude = latitude;
  }

  public double getDistance(double latitude2, double longitude2) {
    // https://www.youtube.com/watch?v=t6NkBRQ2Fz0&t=379s
    double diffLat = Math.abs(latitude2 - latitude) * Math.PI / 180;
    double diffLong = Math.abs(longitude2 - longitude) * Math.PI / 180;
    double a = Math.pow(Math.sin(diffLat/2), 2);
    double b = Math.pow(Math.sin(diffLong/2), 2);
    double c = Math.cos(latitude * Math.PI / 180);
    double d = Math.cos(latitude2 * Math.PI / 180);
    double havTheta = a + (b*c*d);
    double theta = 2 * Math.asin(Math.sqrt(havTheta));
    return theta * 6371;
  }

  @Override
  public String toString() {
    return "GeoLocation{" +
        "longitude=" + longitude +
        ", latitude=" + latitude +
        '}';
  }
}
