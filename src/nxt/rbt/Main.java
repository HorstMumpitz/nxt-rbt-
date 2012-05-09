package nxt.rbt;

import lejos.nxt.LCD;
import lejos.robotics.subsumption.Arbitrator;
import lejos.robotics.subsumption.Behavior;

public class Main {

	public static void main(String[] args) {
		Behavior driveBe = new DriveBe();
		Behavior lightBe = new LightBe();
		Behavior[] bArray = {driveBe,lightBe};
		Arbitrator arby = new Arbitrator(bArray);
		LCD.drawString("Started", 0, 0);
		arby.start();		
	}
}
