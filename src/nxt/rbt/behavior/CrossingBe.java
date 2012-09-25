package nxt.rbt.behavior;

import lejos.nxt.LightSensor;
import lejos.nxt.SensorPort;
import lejos.nxt.comm.RConsole;
import nxt.rbt.graph.DijkstraAlgorithm;
import nxt.rbt.graph.DirectionStates;
import nxt.rbt.graph.Node;
import nxt.rbt.limit.NavigationLimits;
import nxt.rbt.navigation.CrossingCounter;
import nxt.rbt.navigation.LabyrinthNavigator;
import nxt.rbt.navigation.NavigationPilotPose;

public class CrossingBe extends AbstractBehavior{

	LightSensor s1;
	LightSensor s2;
	LightSensor s3;
	boolean hasNode = false;
	
	public CrossingBe(LabyrinthNavigator navigator, NavigationPilotPose pilot) {
		super(navigator, pilot);
		s1 = new LightSensor(SensorPort.S1);
		s2 = new LightSensor(SensorPort.S2);
		s3 = new LightSensor(SensorPort.S3);		
	}
		
	@Override
	public boolean takeControl() {
		
		if ((isInYellow(s2.readValue()) && (isInYellow(s1.readValue()) || isInYellow(s3.readValue()))) ||
				(isInYellow(s3.readValue()) && isInYellow(s1.readValue())) && 
				(isInYellow(s2.readValue()) || !isInYellow(s2.readValue()))) 
			return true;
		else
			return false;
	}

	@Override
	public void action() {
		RConsole.println("Ausgabe: Crossing: s2: " + s2.readValue() +" , s1: " + s1.readValue() + " , s3: " + s3.readValue());	
//		LCD.drawString("Crossing:" ,0, 0);
		CrossingCounter sc =  new CrossingCounter();
//		RConsole.println("Crossing: 1 " + navigator.hasNode());
		hasNode = navigator.hasNode();
		// rechts, hinten, links, geradeaus
		RConsole.println("Ausgabe Pose:1 " + navigator.getPose());
		if(!hasNode) {
			// zum scannen der kreuzung - wie viele abzweigungen vorhanden sind 
			pilot.travel(2.0);
			do {
				pilot.rotate(-1 * NavigationLimits.CROSSING_TURN_RATE_SEARCH);
				sc.addCurrentAngle(NavigationLimits.CROSSING_TURN_RATE_SEARCH);
				if(isInYellow(s2.readValue()) && sc.getCurrentAngle() > (sc.getAngleLastLine() + 20)) {
					int currentAngle = sc.getCurrentAngle();
					if(currentAngle >= 0 && currentAngle <= 20 || currentAngle > 300) {
						sc.setForward(pilot.getPose(), DirectionStates.POSSIBLE);
					} else if(currentAngle > 20 && currentAngle <= 100) {
						sc.setRight(pilot.getPose(), DirectionStates.POSSIBLE);
					} else if(currentAngle > 100 && currentAngle <= 210) {
						sc.setBackward(pilot.getPose(), DirectionStates.TAKEN);
					} else if(currentAngle> 210 && currentAngle <= 300) {
						sc.setLeft(pilot.getPose(), DirectionStates.POSSIBLE);
					}
					sc.addCount();
				}
			} while (sc.getCurrentAngle() < NavigationLimits.COMPLETE_ROTATION);
			navigator.addNode(sc.getDirections());
			RConsole.println("Ausgabe Pose:2 " + navigator.getPose());
//			setNodeForDirections(navigator.getLastNode(), navigator.getCurrentNode());
			RConsole.println("Ausgabe: node Angelegt: " + navigator.getCurrentNode().getId() + " , rechts: " +navigator.getCurrentNode().getDirections()[0]
					+" , links:  " + navigator.getCurrentNode().getDirections()[2] + " , gerade: " + navigator.getCurrentNode().getDirections()[3] + " , " + sc.getDirections().hashCode() );
		}
		
		Node node = navigator.getNodeForPosition();
		if(node != null) {
//			Direction[] states = node.getDirections();
//			RConsole.println("Crossing: 2: " + states + " , " +  states.hashCode());
//			RConsole.println("Ausgabe Pose3 : " + navigator.getPose());
//			if(states != null) {
//				RConsole.println("Crossing: 3: rechts: " + states[0]+" , links:  " +states[2]+" , gerade: "+states[3] +" , " + node.getId());
				// zum abfahren der kreuzung nach rechts und links
				
				if(hasNode)
					pilot.travel(2.0);
				
				
				double currentPose = pilot.getPose();
				
				if (node.getRightDirection(currentPose).getDirectionState() == DirectionStates.POSSIBLE) {
					// right
					sc.resetCurrentAngle();
//					RConsole.println("Crossing: 4: rechts: " + sc.getCurrentAngle());
					do {
						pilot.rotate(-1 * NavigationLimits.CROSSING_TURN_RATE);
						sc.addCurrentAngle(NavigationLimits.CROSSING_TURN_RATE);
					} while (sc.getCurrentAngle() < 20 || (!isInYellow(s2.readValue()) && sc.getCurrentAngle() >= 20));
					node.setRightDirectionState(currentPose, DirectionStates.TAKEN);
//					node.setCurrentDirection(Directions.RIGHT);
				} else if (node.getLeftDirection(currentPose).getDirectionState() == DirectionStates.POSSIBLE) {
					//left
					
					sc.resetCurrentAngle();
//					RConsole.println("Crossing: 4: links: " + sc.getCurrentAngle());
					do {
						pilot.rotate(NavigationLimits.CROSSING_TURN_RATE);
						sc.addCurrentAngle(NavigationLimits.CROSSING_TURN_RATE);
					} while (sc.getCurrentAngle() < 20 || (!isInYellow(s2.readValue()) && sc.getCurrentAngle() >= 20));
					node.setLeftDirectionState(currentPose, DirectionStates.TAKEN);
//					node.setCurrentDirection(Directions.LEFT);
				} else if(node.getForwardDirection(currentPose).getDirectionState() == DirectionStates.POSSIBLE) {
//					RConsole.println("Crossing: 4: geradeaus: ");
					node.setForwardDirection(currentPose, DirectionStates.TAKEN);
//					node.setCurrentDirection(Directions.FORWARD);
					return;
				} else {
					DijkstraAlgorithm alg = new DijkstraAlgorithm(navigator.getGraph());
					alg.execute(navigator.getNodeForPosition());
					if(navigator.isGraphfinished()) {
						// hier kommt die navigation zum start
						alg.getPath(navigator.getStartNode());
						
					} else {
						Node nodeToFinish = navigator.getNodeToFinish();
						if(nodeToFinish != null) {
							alg.getPath(nodeToFinish);
							
							// hier kommt die navigation zum noch nicht fertigen knoten
						} else {
							// hier kommt die navigation zum start
							alg.getPath(navigator.getStartNode());
						}
					}
				}
			}
		}
//	}

	@Override
	public void suppress() {
		
		
	}
	
//	public void setNodeForDirections(Node lasNode, Node currentNode) {
//		// rechts, hinten, links, geradeaus
//		Node[] nodesForDirection = lasNode.getNodesForDirection();
//		switch (lasNode.getCurrentDirection()) {
//		case LEFT:
//			nodesForDirection[2] = currentNode;
//			break;
//		case RIGHT:
//			nodesForDirection[0] = currentNode;
//			break;
//		case FORWARD:
//			nodesForDirection[3] = currentNode;
//			break;
//		default:
//			break;
//		}
//	}
	
}