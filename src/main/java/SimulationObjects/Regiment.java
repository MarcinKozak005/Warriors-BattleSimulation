package SimulationObjects;


import Enums.Alliance;
import Exceptions.CantFindEnemyRegiment;
import Exceptions.CantFindFriendlyRegiment;
import Exceptions.VictoryException;
import Simulation.Handler;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Regiment extends SimulationObject
{
    public int initialRegimentSize = 0;
    public static final double regimentBlockSize = 10;
    // How far an enemy Regiment has to be, to change from move() to attack()
    public static final double regimentInRangeDistance = 100;
    static final double regimentCenterRadius = 150;
    static final double regimentRegroupRadius = 200;
    static final double regimentBorderRadius = 300;


    public String regimentName;
    public boolean inRetreat;
    public List<ArmyUnit> armyUnitList = new LinkedList<>();
    List<ArmyUnit> toRemove = new LinkedList<>();
    Regiment enemyRegiment;
    Handler handler;

    public Regiment(){
        super();
    }
    public Regiment(double x, double y, Alliance alliance, String regimentName, Handler handler) {
        super(x, y, alliance);
        this.handler = handler;
        this.regimentName = regimentName;
        this.inRetreat = false;
        this.maxVelocity = Double.MAX_VALUE;
    }

    public void addArmyUnit(ArmyUnit armyUnit)
    {
        initialRegimentSize++;
        armyUnit.myRegiment = this;
        armyUnit.alliance = this.alliance;
        armyUnitList.add(armyUnit);
    }

    public Optional<ArmyUnit> getFirstArmyUnit()
    {
        try{
            return Optional.of(armyUnitList.get(0));
        }catch (IndexOutOfBoundsException e){
            return Optional.empty();
        }
    }

    @Override
    public void tick() {
        // Find nearest enemy Regiment
        if (this.enemyRegiment == null || !handler.simulationObjectList.contains(this.enemyRegiment)) {
            try {
                this.enemyRegiment = handler.getNearestEnemyFor(this);
            } catch (CantFindEnemyRegiment e) {
                System.out.println("-------- " + this.alliance.toString().toUpperCase() + "'S VICTORY --------");
                throw new VictoryException();
            }
        }

        // Regiment's decision
        // Join with another friend or retreat
        if (armyUnitList.size() < 0.4 * initialRegimentSize) {
            try {
                Regiment nearestFriendlyRegiment = handler.getNearestFriendFor(this);
                for (ArmyUnit armyUnit : this.armyUnitList)
                    nearestFriendlyRegiment.addArmyUnit(armyUnit);

                handler.safeToRemove(this);
            } catch (CantFindFriendlyRegiment e) {
                if (this.armyUnitList.size() < 0.1 * initialRegimentSize && !this.enemyRegiment.inRetreat) {
                    this.inRetreat = true;
                    for (ArmyUnit armyUnit : this.armyUnitList)
                        armyUnit.retreatOrder(this.enemyRegiment);
                }
            }
        }

        if (!this.inRetreat){
            // Chase/pursuit
            if (this.enemyRegiment.inRetreat) {
                for (ArmyUnit armyUnit : this.armyUnitList)
                    armyUnit.chaseOrder(this.enemyRegiment);
            }
            // Regroup
            else if (meanDistanceToRegiment() >= regimentBorderRadius) {
                for (ArmyUnit armyUnit : armyUnitList) armyUnit.regroupOrder();
            }
            // moveToAttack
            else if (this.getDistanceTo(this.enemyRegiment) > regimentInRangeDistance) {
                for (ArmyUnit armyUnit : armyUnitList) armyUnit.moveToAttackOrder(enemyRegiment);

                // Regiment's velocity
                this.maxVelocity = Double.MAX_VALUE;
                for (ArmyUnit armyUnit : armyUnitList)
                    if (armyUnit.getVelocity() < this.getVelocity()) {
                        this.maxVelocity = armyUnit.maxVelocity;
                        this.setVelocityModifier(armyUnit.getVelocityModifier());
                    }

                setDirectionTo(enemyRegiment);
                this.x += velX;
                this.y += velY;
            }
            // attack
            else if (this.getDistanceTo(this.enemyRegiment) <= regimentInRangeDistance) {
                for (ArmyUnit armyUnit : armyUnitList) armyUnit.attackOrder(enemyRegiment);
            }
        }

        for (ArmyUnit armyUnit: armyUnitList) armyUnit.tick();

        this.removeDeadUnits();
        if(this.armyUnitList.size() == 0) this.handler.safeToRemove(this);
    }

    public double meanDistanceToRegiment()
    {
        double sum = 0;
        for (ArmyUnit armyUnit: armyUnitList) sum+=armyUnit.getDistanceTo(this);

        return sum/armyUnitList.size();
    }

    public void safeToRemove(ArmyUnit armyUnit) {
        toRemove.add(armyUnit);
    }


    public <T extends ArmyUnit> void formationSquare(int side, boolean evenlyDistributed, Class<T> clazz){
        double baseX;
        double baseY;
        double step;
        try {
            Predicate<Field> predicate = f -> {
                int m = f.getModifiers();
                return Modifier.isFinal(m) && Modifier.isStatic(m) && Modifier.isPublic(m);
            };
            double blockSize = Arrays.stream(clazz.getFields()).filter(predicate).findFirst().get().getDouble(clazz);

            Constructor<T> constructor = clazz.getConstructor(double.class, double.class);

            if (evenlyDistributed) {
                baseX = this.x - regimentCenterRadius / Math.sqrt(2);
                baseY = this.y - regimentCenterRadius / Math.sqrt(2);
                step = 2 * regimentCenterRadius / (Math.sqrt(2) * (side - 1));
            } else {
                baseX = this.x - side * blockSize;
                baseY = this.y - side * blockSize;
                step = 2 * blockSize;
            }
            for (int i = 0; i < side; i++) {
                for (int j = 0; j < side; j++) {
                    this.addArmyUnit(constructor.newInstance(baseX + i * step, baseY + j * step));
                }
            }
        }
        catch (Exception e){
            System.err.println("FormationSquare Error: "+e);
            System.exit(-1);
        }
    }

    private void removeDeadUnits() {
        armyUnitList.removeAll(toRemove);
        toRemove.clear();
    }

    public void drawCircle(Graphics g, Color c, double radius)
    {
        g.setColor(c);
        g.drawOval((int)(x-radius),(int)(y-radius),(int)radius*2,(int)radius*2);
    }

    @Override
    public void render(Graphics g){
        g.setColor(Color.YELLOW);
        g.fillRect((int) (x - regimentBlockSize/2),(int) (y -regimentBlockSize/2),(int)regimentBlockSize,(int)regimentBlockSize);

        drawCircle(g,Color.GREEN,regimentCenterRadius);
        drawCircle(g,Color.YELLOW,regimentRegroupRadius);
        drawCircle(g,Color.RED,regimentBorderRadius);

        for (ArmyUnit armyUnit: armyUnitList) armyUnit.render(g);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(alliance).append(" ").append(regimentName);
        return stringBuilder.toString();
    }
}
