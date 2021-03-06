import de.voidplus.leapmotion.Arm;
import de.voidplus.leapmotion.Device;
import de.voidplus.leapmotion.Finger;
import de.voidplus.leapmotion.Hand;
import de.voidplus.leapmotion.LeapMotion;
import de.voidplus.leapmotion.Tool;
import g4p_controls.G4P;
import g4p_controls.GAlign;
import g4p_controls.GButton;
import g4p_controls.GCheckbox;
import g4p_controls.GControlMode;
import g4p_controls.GDropList;
import g4p_controls.GEvent;
import g4p_controls.GImageButton;
import g4p_controls.GKnob;
import g4p_controls.GLabel;
import g4p_controls.GPanel;
import g4p_controls.GSlider;
import g4p_controls.GSlider2D;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import processing.core.PApplet;
import processing.core.PSurface;
import processing.core.PVector;
import processing.serial.Serial;

@SuppressWarnings("unused")
public class rwbotClientTest extends PApplet
{
  static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  boolean GRAB_EN = false;
  boolean SERIAL_EN = false;
  boolean YZ_UPDATE = false;
  boolean X_UPDATE = false;
  boolean GRAB_UPDATE = false;
  boolean HAND_UPDATE = false;
  boolean LEAP_EN = false;

  static String VERSION = "2.0";

  static float LIMIT_INIT_X = 0.0F;
  static float LIMIT_MIN_X = -300.0F;
  static float LIMIT_MAX_X = 300.0F;

  static float LIMIT_MIN_Y = 50.0F;
  static float LIMIT_MAX_Y = 330.0F;
  static float LIMIT_INIT_Y = 150.0F;

  static float LIMIT_MIN_Z = -150.0F;
  static float LIMIT_MAX_Z = 250.0F;
  static float LIMIT_INIT_Z = 100.0F;

  static float LIMIT_MIN_HAND = 0.0F;
  static float LIMIT_MAX_HAND = 180.0F;
  static float LIMIT_INIT_HAND = 90.0F;

  String FIRMWARE_VERSION = "N/A";

  float current_x = 0.0F;
  float current_y = 0.0F;
  float current_z = 0.0F;
  float current_h = 0.0F;
  int limit_leap_min_z = -10;
  PrintWriter output;

  public rwbotClientTest() {}

  public void settings() {
    size(1024, 500, "processing.awt.PGraphicsJava2D");
  }

  public void setup() {
    output = createWriter("logs.txt");
     initLeapMotion();
    createGUI();
    customGUI();
    reset();
  }

  public void draw() {
    background(255);
     if (LEAP_EN)
       leapmotion();
    updatePos();
  }

  public void updatePos() {
    setCurrentPosition();
    if (GRAB_UPDATE) {
      setPump();
      GRAB_UPDATE = false;
    }
    if (X_UPDATE) {
      setPosition();
      X_UPDATE = false;
    }
    if (HAND_UPDATE) {
      setWrist();
      HAND_UPDATE = false;
    }
    if (YZ_UPDATE) {
      setPosition();
      YZ_UPDATE = false;
    }
  }

  public void setCurrentPosition() {
    current_x = slider2d_xy.getValueXF();
    current_z = slider_z_axis.getValueF();
    current_y = slider2d_xy.getValueYF();
    current_h = knob_hand_axis.getValueF();
  }

  public void setUIValue(float x, float y, float z, float h)
  {
    slider2d_xy.setValueX(x);
    slider2d_xy.setValueY(y);
    slider_z_axis.setValue(z);
    knob_hand_axis.setValue(h);
  }

  public void initPort() {
    String portName = droplist_serial.getSelectedText();
    printf(portName);
    try {
      uPort = new Serial(this, portName, 115200);
      printf("Connecting to Port:" + portName);
      long startTime = System.currentTimeMillis();
      delay(2000);
      while (System.currentTimeMillis() - startTime < 5000L) {
        while (uPort.available() > 0) {
          String line = uPort.readStringUntil(10);
          println("line:" + line);
          if (line.startsWith("@1")) {
            SERIAL_EN = true;
            button_connect_port.setText("Disconnect");
            break;
          }
        }
        if (SERIAL_EN)
          break;
      }
      if (SERIAL_EN) {
        String msg = "#1 P203\n";
        uPort.write(msg);
        startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 5000L) {
          while (uPort.available() > 0) {
            String line = uPort.readStringUntil(10);
            if (line.startsWith("$1 OK")) {
              FIRMWARE_VERSION = line.split(" ")[2];
              label_firmware_vesion_label.setText(FIRMWARE_VERSION);
              break;
            }
          }
          if (FIRMWARE_VERSION != "N/A")
            break;
        }
        reset();
      }
      else {
        releasePort();
        G4P.showMessage(this, "Firmware Not Correct!", "ERROR", 0);
      }
    }
    catch (Exception e) {
      G4P.showMessage(this, "Can't initialize Port" + portName + "Error: " + e, "ERROR", 0);
    }
  }

  public void releasePort()
  {
    try {
      SERIAL_EN = false;
      uPort.stop();
      button_connect_port.setText("Connect");
      label_firmware_vesion_label.setText("N/A");
    } catch (Exception localException) {
      G4P.showMessage(this, "Can't Disconnect Port", "ERROR", 0);
    }
  }

  public String[] getUArmPorts() {
    List<String> ports = new ArrayList<String>();
    for (String port : Serial.list())
    {
      String idProduct = (String)Serial.getProperties(port).get("idProduct");
      if ((!port.startsWith("/dev/cu.")) && (idProduct != null) && (idProduct.equals("6001")))
      {

        ports.add(port);
      }
    }
    String[] stockArr = new String[ports.size()];
    return (String[])ports.toArray(stockArr);
  }

  public void reset() {
    setUIValue(LIMIT_INIT_X, LIMIT_INIT_Y, LIMIT_INIT_Z, LIMIT_INIT_HAND);
    button_grab.setText("Catch");
    GRAB_EN = false;
    GRAB_UPDATE = true;
    YZ_UPDATE = true;
    X_UPDATE = true;
    HAND_UPDATE = true;
    GRAB_UPDATE = true;
    updatePos();
  }


  public void customGUI()
  {
    slider2d_xy.setEasing(5.0F);
    droplist_serial.setItems(getUArmPorts(), 0);
  }

  public String roundTwoDecimals(float d) {
    DecimalFormat twoDForm = new DecimalFormat("#.#");
    return twoDForm.format(d);
  }

  public void printf(String msg) {
    println(msg);
    msg = sdf.format(Calendar.getInstance().getTime()) + ": " + msg;
    output.println(msg);
    output.flush();
  }


  Serial uPort;

  GImageButton logo_icon;

  GImageButton logo_text;

  GSlider2D slider2d_xy;

  GLabel label_y_axis;

  GLabel val_x_axis;

  GLabel label_z_axis;

  GLabel val_y_axis;
  GSlider slider_z_axis;
  GLabel label_x_axis;
  GKnob knob_hand_axis;
  public void logo_icon_Click(GImageButton source, GEvent event) {}

  public void logo_text_click(GImageButton source, GEvent event) {}

  public void slider2d_xy_changed(GSlider2D source, GEvent event)
  {
    val_x_axis.setText(roundTwoDecimals(slider2d_xy.getValueXF()));
    val_y_axis.setText(roundTwoDecimals(slider2d_xy.getValueYF()));
    YZ_UPDATE = true;
    X_UPDATE = true;
  }

  public void slider_z_axis_changed(GSlider source, GEvent event)
  {
    YZ_UPDATE = true;
  }

  public void knob_hand_axis_changed(GKnob source, GEvent event)
  {
    val_hand_axis.setText(roundTwoDecimals(knob_hand_axis.getValueF()));
    HAND_UPDATE = true;
  }


  public void setting_panel_clicked(GPanel source, GEvent event) {}

  public void connect_serial_clicked(GButton source, GEvent event)
  {
    printf("droplist_serial - GDropList >> GEvent." + event + " @ " + millis());
    if (!SERIAL_EN) {
      initPort();
    }
    else {
      releasePort();
    }
  }

  public void button_rescan_port_clicked(GButton source, GEvent event)
  {
    printf("button1 - GButton >> GEvent." + event + " @ " + millis());
    droplist_serial.setItems(getUArmPorts(), 0);
  }

   public void cb_leapmotion_clicked(GCheckbox source, GEvent event) {
     printf("cb_leapmotion - GCheckbox >> GEvent." + event + " @ " + millis());
     LEAP_EN = cb_leapmotion.isSelected();
   }

  public void slider_min_z_changed(GSlider source, GEvent event) {
    printf("slider_min_z - GSlider >> GEvent." + event + " @ " + millis());
    limit_leap_min_z = slider_min_z.getValueI();
  }

  public void button_grab_clicked(GButton source, GEvent event)
  {
    GRAB_UPDATE = true;
    if (!GRAB_EN) {
      button_grab.setText("Release");
      GRAB_EN = true;
    } else {
      button_grab.setText("Catch");
      GRAB_EN = false;
    }
  }

  public void button_reset_clicked(GButton source, GEvent event)
  {
    reset();
  }

  public void createGUI()
  {
    G4P.messagesEnabled(false);
    G4P.setGlobalColorScheme(6);
    G4P.setCursor(0);
    surface.setTitle("Rwbot Control Panel");
    logo_icon = new GImageButton(this, 400.0F, 10.0F, 114.0F, 40.0F, new String[] { "logo.png", "logo.png", "logo.png" });
    GLabel label_version = new GLabel(this, 520.0F, 20.0F, 100.0F, 20.0F);
    label_version.setText("v" + VERSION);
    slider2d_xy = new GSlider2D(this, 115.0F, 75.0F, 520.0F, 420.0F);
    slider2d_xy.setLimitsX(LIMIT_INIT_X, LIMIT_MIN_X, LIMIT_MAX_X);
    slider2d_xy.setLimitsY(LIMIT_INIT_Y, LIMIT_MAX_Y, LIMIT_MIN_Y);
    slider2d_xy.setNumberFormat(0, 0);
    slider2d_xy.setLocalColorScheme(5);
    slider2d_xy.setOpaque(true);
    slider2d_xy.addEventHandler(this, "slider2d_xy_changed");
    label_x_axis = new GLabel(this, 210.0F, 50.0F, 50.0F, 20.0F);
    label_x_axis.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
    label_x_axis.setText("X AXIS: ");
    label_x_axis.setTextBold();
    label_x_axis.setLocalColorScheme(5);
    label_x_axis.setOpaque(false);
    val_x_axis = new GLabel(this, 270.0F, 50.0F, 50.0F, 20.0F);
    val_x_axis.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
    val_x_axis.setText("0");
    val_x_axis.setTextBold();
    val_x_axis.setLocalColorScheme(5);
    val_x_axis.setOpaque(false);
    label_y_axis = new GLabel(this, 400.0F, 50.0F, 50.0F, 20.0F);
    label_y_axis.setText("Y AXIS: ");
    label_y_axis.setTextBold();
    label_y_axis.setLocalColorScheme(5);
    label_y_axis.setOpaque(false);
    val_y_axis = new GLabel(this, 450.0F, 50.0F, 80.0F, 20.0F);
    val_y_axis.setText("0");
    val_y_axis.setTextBold();
    val_y_axis.setLocalColorScheme(5);
    val_y_axis.setOpaque(false);
    slider_z_axis = new GSlider(this, 110.0F, 75.0F, 420.0F, 100.0F, 15.0F);
    slider_z_axis.setShowValue(true);
    slider_z_axis.setShowLimits(true);
    slider_z_axis.setLimits(LIMIT_INIT_Z, LIMIT_MAX_Z, LIMIT_MIN_Z);
    slider_z_axis.setShowTicks(true);
    slider_z_axis.setEasing(5.0F);
    slider_z_axis.setRotation(1.5707964F, GControlMode.CORNER);
    slider_z_axis.setTextOrientation(-1);

    slider_z_axis.setLocalColorScheme(5);
    slider_z_axis.setOpaque(false);
    slider_z_axis.addEventHandler(this, "slider_z_axis_changed");
    label_z_axis = new GLabel(this, 15.0F, 50.0F, 50.0F, 20.0F);
    label_z_axis.setText("Z AXIS:");
    label_z_axis.setTextBold();
    label_z_axis.setLocalColorScheme(5);
    label_z_axis.setOpaque(false);
    knob_hand_axis = new GKnob(this, 700.0F, 75.0F, 200.0F, 200.0F, 0.8F);
    knob_hand_axis.setTurnRange(180.0F, 360.0F);
    knob_hand_axis.setTurnMode(1281);
    knob_hand_axis.setShowArcOnly(true);
    knob_hand_axis.setOverArcOnly(true);
    knob_hand_axis.setIncludeOverBezel(false);
    knob_hand_axis.setShowTrack(false);
    knob_hand_axis.setLimits(LIMIT_INIT_HAND, LIMIT_MIN_HAND, LIMIT_MAX_HAND);
    knob_hand_axis.setShowTicks(true);

    knob_hand_axis.setLocalColorScheme(5);
    knob_hand_axis.setOpaque(false);
    knob_hand_axis.addEventHandler(this, "knob_hand_axis_changed");
    label_hand_axis = new GLabel(this, 725.0F, 50.0F, 80.0F, 20.0F);
    label_hand_axis.setText("Hand AXIS:");
    label_hand_axis.setTextBold();
    label_hand_axis.setLocalColorScheme(5);
    label_hand_axis.setOpaque(false);
    val_hand_axis = new GLabel(this, 810.0F, 50.0F, 50.0F, 20.0F);
    val_hand_axis.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
    val_hand_axis.setText("0");
    val_hand_axis.setTextBold();
    val_hand_axis.setLocalColorScheme(5);
    val_hand_axis.setOpaque(false);
    setting_panel = new GPanel(this, 650.0F, 280.0F, 350.0F, 210.0F, "Setting");
    setting_panel.setCollapsible(false);
    setting_panel.setDraggable(false);
    setting_panel.setText("Setting");
    setting_panel.setTextBold();
    setting_panel.setLocalColorScheme(5);
    setting_panel.setOpaque(true);
    setting_panel.addEventHandler(this, "setting_panel_clicked");
    droplist_serial = new GDropList(this, 10.0F, 25.0F, 180.0F, 80.0F, 3);
    droplist_serial.setItems(loadStrings("list_920274"), 0);
    droplist_serial.setLocalColorScheme(5);


    button_rescan_port = new GButton(this, 270.0F, 25.0F, 70.0F, 20.0F);
    button_rescan_port.setText("Rescan");
    button_rescan_port.setTextBold();
    button_rescan_port.setLocalColorScheme(5);
    button_rescan_port.addEventHandler(this, "button_rescan_port_clicked");

    button_connect_port = new GButton(this, 200.0F, 25.0F, 70.0F, 20.0F);
    button_connect_port.setText("Connect");
    button_connect_port.setTextBold();
    button_connect_port.setLocalColorScheme(5);
    button_connect_port.addEventHandler(this, "connect_serial_clicked");

    GLabel label_firmware_vesion = new GLabel(this, 10.0F, 55.0F, 120.0F, 29.0F);
    label_firmware_vesion.setText("Firmware Version:");

    label_firmware_vesion.setLocalColorScheme(5);
    label_firmware_vesion.setOpaque(false);
    label_firmware_vesion_label = new GLabel(this, 140.0F, 55.0F, 200.0F, 29.0F);
    label_firmware_vesion_label.setText("N/A");
    label_firmware_vesion_label.setTextBold();
    label_firmware_vesion_label.setLocalColorScheme(5);
    label_firmware_vesion_label.setOpaque(false);
    label_firmware_vesion_label.setTextBold();
     cb_leapmotion = new GCheckbox(this, 10.0F, 110.0F, 200.0F, 20.0F);
     cb_leapmotion.setTextAlign(GAlign.LEFT, GAlign.MIDDLE);
     cb_leapmotion.setText("Enable Leap Motion Control");
     cb_leapmotion.setTextBold();
     cb_leapmotion.setLocalColorScheme(5);
     cb_leapmotion.setOpaque(false);
     cb_leapmotion.addEventHandler(this, "cb_leapmotion_clicked");
    slider_min_z = new GSlider(this, 10.0F, 140.0F, 200.0F, 50.0F, 10.0F);
    slider_min_z.setShowValue(true);
    slider_min_z.setShowLimits(true);
    slider_min_z.setLimits(LIMIT_INIT_Z, LIMIT_MIN_Z, LIMIT_MAX_Z);
    slider_min_z.setShowTicks(true);

    slider_min_z.setNumberFormat(0, 0);
    slider_min_z.setLocalColorScheme(5);
    slider_min_z.setOpaque(false);
    slider_min_z.addEventHandler(this, "slider_min_z_changed");
    label_leap_min_z = new GLabel(this, 215.0F, 150.0F, 80.0F, 29.0F);
     label_leap_min_z.setText("Minmum Z");
     label_leap_min_z.setTextBold();
     label_leap_min_z.setLocalColorScheme(5);
     label_leap_min_z.setOpaque(false);
    setting_panel.addControl(droplist_serial);
    setting_panel.addControl(button_rescan_port);
    setting_panel.addControl(button_connect_port);
    setting_panel.addControl(label_firmware_vesion);
    setting_panel.addControl(label_firmware_vesion_label);
     setting_panel.addControl(cb_leapmotion);
    setting_panel.addControl(slider_min_z);
    setting_panel.addControl(label_leap_min_z);
    button_grab = new GButton(this, 740.0F, 210.0F, 120.0F, 40.0F);
    button_grab.setText("Catch");
    button_grab.setTextBold();
    button_grab.setLocalColorScheme(5);
    button_grab.addEventHandler(this, "button_grab_clicked");
    button_reset = new GButton(this, 930.0F, 10.0F, 50.0F, 30.0F);
    button_reset.setText("RESET");
    button_reset.setTextBold();
    button_reset.setLocalColorScheme(5);
    button_reset.addEventHandler(this, "button_reset_clicked");
  }


  GLabel label_firmware_vesion_label;
  GLabel label_hand_axis;
  GLabel val_hand_axis;
  GPanel setting_panel;
  GDropList droplist_serial;
  GButton button_rescan_port;
  GButton button_connect_port;
  GCheckbox cb_leapmotion;
  GSlider slider_min_z;
  GLabel label_leap_min_z;
  GButton button_grab;
  GButton button_reset;


  
  public void setPosition()
  {
    String msg = "#1 G0 X" + roundTwoDecimals(current_x) + " Y" + roundTwoDecimals(current_y) + " Z" + roundTwoDecimals(current_z) + " F0\n";
    printf(msg);
    if (SERIAL_EN)
      uPort.write(msg);
  }

  public void setPump() {
    int pump_status = 0;
    if (GRAB_EN) {
      pump_status = 1;
    } else
      pump_status = 0;
    String msg_pump = "#1 M231 V" + pump_status + "\n";
    String msg_gripper = "#1 M232 V" + pump_status + "\n";
    printf(msg_pump);
    printf(msg_gripper);
    if (SERIAL_EN) {
      uPort.write(msg_pump);
      uPort.write(msg_gripper);
    }
  }

  public void setWrist() {
    String msg = "#1 G202 N3 V" + current_h + "\n";
    printf(msg);
    if (SERIAL_EN)
      uPort.write(msg);
  }
  

   LeapMotion leap;

   public void initLeapMotion()
   {
     leap = new LeapMotion(this);
   }
  
   public void leapmotion() {
     background(255);
  
     leap.getFrameRate();
  
     PVector hand_position;
  
     if (!leap.getHands().isEmpty())
     {
       Hand hand = (Hand)leap.getHands().toArray()[0];
  
       hand.getId();
       hand_position = hand.getPosition();
  
       PVector hand_stabilized = hand.getStabilizedPosition();
       hand.getDirection();
       PVector hand_dynamics = hand.getDynamics();
  
       hand.getRoll();
       hand.getPitch();
       hand.getYaw();
       hand.isLeft();
       hand.isRight();
       float hand_grab = hand.getGrabStrength();
       hand.getPinchStrength();
       hand.getTimeVisible();
       hand.getSpherePosition();
       hand.getSphereRadius();
  
       println(hand_dynamics);
       println(hand_grab);
  
       hand.getThumb();
  
       hand.getIndexFinger();
  
       hand.getMiddleFinger();
  
       hand.getRingFinger();
  
       hand.getPinkyFinger();
  
       hand.draw(8.0F);
       tint(255, 200.0F);
  
       int xP = (int)map(constrain(hand_stabilized.x, 0.0F, 1000.0F), 0.0F, 1000.0F, -300.0F, 300.0F);
       int zP = (int)map(constrain(hand_stabilized.y, 50.0F, 250.0F), 150.0F, 250.0F, 250.0F, limit_leap_min_z);
       int yP = (int)map(constrain(hand_stabilized.z, 0.0F, 80.0F), 0.0F, 80.0F, 50.0F, 330.0F);
  
  
       setUIValue(xP, yP, zP, 90.0F);
       if ((hand_grab >= 0.8F) && (!GRAB_EN))
       {
         GRAB_EN = true;
         GRAB_UPDATE = true;
       }
       else if ((hand_grab < 0.8F) && (GRAB_EN))
       {
         GRAB_EN = false;
         GRAB_UPDATE = true;
       }
  
       if (hand.hasArm()) {
         Arm arm = hand.getArm();
         arm.getWidth();
         arm.getWristPosition();
         arm.getElbowPosition();
       }
  
       for (Finger finger : hand.getFingers())
       {
  
         finger.getId();
         finger.getPosition();
         finger.getStabilizedPosition();
         finger.getVelocity();
         finger.getDirection();
         finger.getTimeVisible();
  
         switch (finger.getType())
         {
         case 0:
           break;
         case 1:
           break;
         case 2:
           break;
         case 3:
           break;
         }
  
         finger.getDistalBone();
  
         finger.getIntermediateBone();
  
         finger.getProximalBone();
  
         finger.getMetacarpalBone();
  
  
         int touch_zone = finger.getTouchZone();
         finger.getTouchDistance();
  
         switch (touch_zone)
         {
         case -1:
           break;
         case 0:
  
         }
  
       }
  
       for (Tool tool : hand.getTools())
       {
  
         tool.getId();
         tool.getPosition();
         tool.getStabilizedPosition();
         tool.getVelocity();
         tool.getDirection();
         tool.getTimeVisible();
  
  
         int touch_zone = tool.getTouchZone();
         tool.getTouchDistance();
  
         switch (touch_zone)
         {
         case -1:
           break;
         case 0:
  
         }
  
       }
     }
  
  
     for (Device device : leap.getDevices()) {
       device.getHorizontalViewAngle();
       device.getVerticalViewAngle();
       device.getRange();
     }
   }
  
   public void leapOnInit() {}
   public void leapOnConnect() {}
   public void leapOnFrame() {}
   public void leapOnDisconnect() {}
   public void leapOnExit() {}




  public static void main(String[] passedArgs) { String[] appletArgs = { "rwbotClient" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
