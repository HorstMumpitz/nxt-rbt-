package nxt.rbt.behavior;

import lejos.nxt.LightSensor;
import lejos.nxt.SensorPort;
import lejos.nxt.comm.RConsole;
import nxt.rbt.graph.DijkstraAlgorithm;
import nxt.rbt.graph.Direction;
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
		RConsole.println("Ausgabe: Pose:1 " + navigator.getPose());
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
				RConsole.println("Ausgabe: node bekannt: 1");
				if(hasNode)
					pilot.travel(2.0);
				
				RConsole.println("Ausgabe: node bekannt: 2");
				double currentPose = pilot.getPose();
				
				RConsole.println("Ausgabe: node bekannt: 3");
				if (node.getRightDirection(currentPose)!= null && node.getRightDirection(currentPose).getDirectionState() == DirectionStates.POSSIBLE) {
					RConsole.println("Ausgabe: node bekannt: 4");
					// right
					sc.resetCurrentAngle();
//					RConsole.println("Crossing: 4: rechts: " + sc.getCurrentAngle());
					do {
						pilot.rotate(-1 * NavigationLimits.CROSSING_TURN_RATE);
						sc.addCurrentAngle(NavigationLimits.CROSSING_TURN_RATE);
					} while (sc.getCurrentAngle() < 20 || (!isInYellow(s2.readValue()) && sc.getCurrentAngle() >= 20));
					node.setRightDirectionState(currentPose, DirectionStates.TAKEN);
//					node.setCurrentDirection(Directions.RIGHT);
				} else if (node.getLeftDirection(currentPose) != null && node.getLeftDirection(currentPose).getDirectionState() == DirectionStates.POSSIBLE) {
					//left
					RConsole.println("Ausgabe: node bekannt: 5");
					sc.resetCurrentAngle();
//					RConsole.println("Crossing: 4: links: " + sc.getCurrentAngle());
					do {
						pilot.rotate(NavigationLimits.CROSSING_TURN_RATE);
						sc.addCurrentAngle(NavigationLimits.CROSSING_TURN_RATE);
					} while (sc.getCurrentAngle() < 20 || (!isInYellow(s2.readValue()) && sc.getCurrentAngle() >= 20));
					node.setLeftDirectionState(currentPose, DirectionStates.TAKEN);
//					node.setCurrentDirection(Directions.LEFT);
				} else if(node.getForwardDirection(currentPose) != null && node.getForwardDirection(currentPose).getDirectionState() == DirectionStates.POSSIBLE) {
//					RConsole.println("Crossing: 4: geradeaus: ");
					RConsole.println("Ausgabe: node bekannt: 6");
					pilot.travel(2.0);
					node.setForwardDirection(currentPose, DirectionStates.TAKEN);
//					node.setCurrentDirection(Directions.FORWARD);
					return;
				} else {
					RConsole.println("Ausgabe: node bekannt: 7");
					if(navigator.isNavigateToNode() || navigator.isNavigateToFinish()) {
						RConsole.println("Ausgabe: node bekannt: 8");
						if(navigator.getCurrentPosNode() < navigator.getPath().size()) {
							RConsole.println("Ausgabe: node bekannt: 9");
							Node currentNode = navigator.getPath().get(navigator.getCurrentPosNode());
							Direction direction = navigator.getDirectionToDrive(currentNode);
							driveToDirection(direction);
							if (navigator.getCurrentPosNode() >= navigator.getPath().size() -1) {
								RConsole.println("Ausgabe: node bekannt: 10");
								driveToDirection(direction);
								if(currentNode.isStartNode()) {
									RConsole.println("Ausgabe: node bekannt: 11");
									pilot.travel(4.0);
									sc.resetCurrentAngle();
									do {
										sc.addCurrentAngle(NavigationLimits.CROSSING_TURN_RATE_ENDLINE);
										pilot.rotate(NavigationLimits.CROSSING_TURN_RATE_ENDLINE);
	//									RConsole.println("Ausgabe: Endline drehen: s2: " + s2.readValue() +" , wnkel: " + sc.getCurrentAngle()) ;
									} while (sc.getCurrentAngle() < 100 || (!isInYellow(s2.readValue()) && sc.getCurrentAngle() >= 100));
									RConsole.println("Ausgabe: node bekannt: 12");
									DijkstraAlgorithm alg = new DijkstraAlgorithm(navigator.getGraph());
									alg.execute(navigator.getStartNode());
									navigator.setPath(alg.getPath(navigator.getFinishNode()));
									navigator.setNavigateToFinish(true);
								} else {
									RConsole.println("Ausgabe: node bekannt: 13");
									navigator.setNavigateToNode(false);
								}
						}
					} else {
						RConsole.println("Ausgabe: node bekannt: 14");
						DijkstraAlgorithm alg = new DijkstraAlgorithm(navigator.getGraph());
						alg.execute(navigator.getNodeForPosition());
						if(navigator.isGraphfinished()) {
							RConsole.println("Ausgabe: node bekannt: 15");
							// hier kommt die navigation zum start
							navigator.setPath(alg.getPath(navigator.getStartNode()));
							navigator.setNavigateToNode(true);
							Node currentNode = navigator.getPath().get(navigator.getCurrentPosNode());
							Direction direction = navigator.getDirectionToDrive(currentNode);
							driveToDirection(direction);
						
						} else {
							RConsole.println("Ausgabe: node bekannt: 16");
							Node nodeToFinish = navigator.getNodeToFinish();
							if(nodeToFinish != null) {
								RConsole.println("Ausgabe: node bekannt: 17");
								navigator.setPath(alg.getPath(nodeToFinish));
								navigator.setNavigateToNode(true);
								Node currentNode = navigator.getPath().get(navigator.getCurrentPosNode());
								Direction direction = navigator.getDirectionToDrive(currentNode);
								driveToDirection(direction);
								// hier kommt die navigation zum noch nicht fertigen knoten
							} else {
								RConsole.println("Ausgabe: node bekannt: 18");
								// hier kommt die navigation zum start
								navigator.setPath(alg.getPath(navigator.getStartNode()));
								navigator.setNavigateToNode(true);
								Node currentNode = navigator.getPath().get(navigator.getCurrentPosNode());
								Direction direction = navigator.getDirectionToDrive(currentNode);
								driveToDirection(direction);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void suppress() {
		
		
	}
	
	public void driveToDirection(Direction direction) {
		CrossingCounter sc =  new CrossingCounter();
		Node node = navigator.getNodeForPosition();
		if (node.getRightDirection(direction.getPose()) != null) {
			// right
			sc.resetCurrentAngle();
//			RConsole.println("Crossing: 4: rechts: " + sc.getCurrentAngle());
			do {
				pilot.rotate(-1 * NavigationLimits.CROSSING_TURN_RATE);
				sc.addCurrentAngle(NavigationLimits.CROSSING_TURN_RATE);
			} while (sc.getCurrentAngle() < 20 || (!isInYellow(s2.readValue()) && sc.getCurrentAngle() >= 20));
			navigator.incrementCurrentPosNode();
//			node.setCurrentDirection(Directions.RIGHT);
		} else if (node.getLeftDirection(direction.getPose()) != null) {
			//left
			sc.resetCurrentAngle();
//			RConsole.println("Crossing: 4: links: " + sc.getCurrentAngle());
			do {
				pilot.rotate(NavigationLimits.CROSSING_TURN_RATE);
				sc.addCurrentAngle(NavigationLimits.CROSSING_TURN_RATE);
			} while (sc.getCurrentAngle() < 20 || (!isInYellow(s2.readValue()) && sc.getCurrentAngle() >= 20));
			navigator.incrementCurrentPosNode();
//			node.setCurrentDirection(Directions.LEFT);
		} else if(node.getForwardDirection(direction.getPose()) != null) {
//			RConsole.println("Crossing: 4: geradeaus: ");
//			node.setCurrentDirection(Directions.FORWARD);
			pilot.travel(2.0);
			navigator.incrementCurrentPosNode();
			return;
		}
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