import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class ExpertComp extends Player
{
    private String enemyName;
    private int countGames;

    private HitsMap hitsMap;

    private final File stateFile;

    public ExpertComp(String name) {
        this(name, new File(name + ".txt"));
    }

    public ExpertComp(String name, File stateFile) {
        super(name);
        this.stateFile = stateFile;
        populateShips();
    }

    //------------------------------------------------------------------------------------------------------------------
    private boolean sunkA = false,sunkD = false,sunkC = false,sunkS = false,sunkP = false;
    private ArrayList<Location> hitsAC = new ArrayList<>();
    private ArrayList<Location> hitsDE = new ArrayList<>();
    private ArrayList<Location> hitsCR = new ArrayList<>();
    private ArrayList<Location> hitsSU = new ArrayList<>();
    private ArrayList<Location> hitsPB = new ArrayList<>();
    private int[][] gridAC, gridDE, gridCR, gridSU, gridPB;

    private int turns;

    ExpertComp player;

    private HeatMap mappa;

    private void shipScar(Ship s, Location l)
    {
        if(s.getLocations().size() == 5)
            this.hitsAC.add(l);
        if(s.getLocations().size() == 4)
            this.hitsDE.add(l);
        if(s.getClass().getName().equals(Cruiser.class.getName()))
            this.hitsCR.add(l);
        if(s.getClass().getName().equals(Submarine.class.getName()))
            this.hitsSU.add(l);
        if (s.getLocations().size() == 2)
            this.hitsPB.add(l);
    }


    private void LD(Player enemy, Location loa)
    {
        if(enemy.getShip(loa).getClass().getName().equals(AircraftCarrier.class.getName()))
        {
            add(mappa.getTuneAC(), shipToGrid(hitsAC));
            this.sunkA = true;
        }
        if(enemy.getShip(loa).getClass().getName().equals(Destroyer.class.getName()))
        {
            add(mappa.getTuneDE(), shipToGrid(hitsDE));
            this.sunkD = true;
        }
        if(enemy.getShip(loa).getClass().getName().equals(Cruiser.class.getName()))
        {
            add(mappa.getTuneCR(), shipToGrid(hitsCR));
            this.sunkC = true;
        }
        if(enemy.getShip(loa).getClass().getName().equals(Submarine.class.getName()))
        {
            add(mappa.getTuneSU(), shipToGrid(hitsSU));
            this.sunkS = true;
        }
        if(enemy.getShip(loa).getClass().getName().equals(PatrolBoat.class.getName()))
        {
            add(mappa.getTunePB(), shipToGrid(hitsPB));
            this.sunkP = true;
        }
    }
    private void LOD()
    {
        mappa.setAC(sunkA);
        mappa.setDE(sunkD);
        mappa.setCR(sunkC);
        mappa.setSU(sunkS);
        mappa.setPB(sunkP);
    }

    private ArrayList<Location> shipToHit()
    {
        if(hitsAC.size() != 0 && hitsAC.size() < 5  && !sunkA)
            return hitsAC;
        if(hitsDE.size() != 0 && hitsDE.size() < 4 && !sunkD)
            return hitsDE;
        if(hitsCR.size() != 0 && hitsCR.size() < 3 && !sunkC)
            return hitsCR;
        if(hitsSU.size() != 0 && hitsSU.size() < 3 && !sunkS)
            return hitsSU;
        return hitsPB;
    }

    private int spaceBTWN()
    {
        if(!sunkP)
            return 2;
        if(!sunkS)
            return 3;
        if(!sunkC)
            return 3;
        if(!sunkD)
            return 4;
        return 5;
    }

    private boolean shouldHunt()
    {
        if(hitsAC.size() != 0 && hitsAC.size() < 5 && !sunkA)
            return true;
        if(hitsDE.size() != 0 && hitsDE.size() < 4 && !sunkD)
            return true;
        if(hitsCR.size() != 0 && hitsCR.size() < 3 && !sunkC)
            return true;
        if(hitsSU.size() != 0 && hitsSU.size() < 3 && !sunkS)
            return true;
        return (hitsPB.size() == 1 && !sunkP);
    }

    private boolean ATKSequence(Player enemy, Location loa)
    {
        turns ++;
        mappa.placeMiss(loa, enemy.hasShipAtLocation(loa));
        if (enemy.hasShipAtLocation(loa))
        {
//            System.out.println(loa.getRow() + " " + loa.getCol() + " is a hit");
            shipScar(enemy.getShip(loa),loa);
            super.getGuessBoard()[loa.getRow()][loa.getCol()] = 1;
            mappa.placeMiss(loa, enemy.hasShipAtLocation(loa));
            enemy.getShip(loa).takeHit(loa);
            if (enemy.getShip(loa).isSunk())
            {
                LD(enemy,loa);
//                System.out.println(enemy.getShip(loa) + " is sunk");
                enemy.removeShip(enemy.getShip(loa));
                if(turns > 2)
                    save();
                return true;
            }
        }
        else
        {
//            System.out.println(loa.getRow() + " " + loa.getCol() + " is a miss");
            super.getGuessBoard()[loa.getRow()][loa.getCol()] = -1;
            mappa.placeMiss(loa, enemy.hasShipAtLocation(loa));
        }
        return false;
    }

    private boolean lookRow(ArrayList<Location> loc)
    {
        return (loc.get(0).getRow() == loc.get(1).getRow());
    }


    private int maxOrMinRow(ArrayList<Location> loc, int i)
    {
        int rtn = loc.get(0).getRow();

        if(i > 0)
        {
            for (Location l : loc)
                if (l.getRow() > rtn)
                    rtn = l.getRow();
        }
        else
        {
            for (Location l : loc)
                if (l.getRow() < rtn)
                    rtn = l.getRow();
        }
        return rtn;
    }

    private int maxOrMinCol(ArrayList<Location> loc, int i)
    {
        int rtn =loc.get(0).getCol();

        if(i > 0)
        {
            for (Location l : loc)
                if (l.getCol() > rtn)
                    rtn = l.getCol();
        }
        else
        {
            for (Location l : loc)
                if (l.getCol() < rtn)
                    rtn = l.getCol();
        }
        return rtn;
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Randomly chooses a Location that has not been
     *   attacked (Location loc is ignored).  Marks
     *   the attacked Location on the guess board
     *   with a positive number if the enemy Player
     *   controls a ship at the Location attacked;
     *   otherwise, if the enemy Player does not
     *   control a ship at the attacked Location,
     *   guess board is marked with a negative number.
     *
     * If the enemy Player controls a ship at the attacked
     *   Location, the ship must add the Location to its
     *   hits taken.  Then, if the ship has been sunk, it
     *   is removed from the enemy Player's list of ships.
     *
     * Return true if the attack resulted in a ship sinking;
     *   false otherwise.
     *
     * @param enemy The Player to attack.
     * @param loc The Location to attack.
     * @return boolean of if they sunk a ship
     */
    @Override
    public boolean attack(Player enemy, Location loc)
    {
        if(!enemy.getName().equals(enemyName))
        {
            enemyName = enemy.getName();
            countGames = 1;
            mappa = new HeatMap();
            hitsMap = new HitsMap();
        }

        mappa.addHM(countGames);
        LOD();
        shouldHunt();
        Random ran = new Random();

//        System.out.println(shouldHunt());
        if(shouldHunt())
        {
//            System.out.println(enemy.getShip(shipToHit().get(0)).getClass().getName());
            if(shipToHit().size() == 1)
            {
//                System.out.println("ship is short");
                if(shipToHit().get(0).getRow() < 9 && getGuessBoard()[shipToHit().get(0).getRow() + 1][shipToHit().get(0).getCol()] == 0)
                    return ATKSequence(enemy, new Location(shipToHit().get(0).getRow() + 1,shipToHit().get(0).getCol()));

                if(shipToHit().get(0).getCol() > 0 && getGuessBoard()[shipToHit().get(0).getRow()][shipToHit().get(0).getCol() - 1] == 0)
                    return ATKSequence(enemy, new Location(shipToHit().get(0).getRow(),shipToHit().get(0).getCol() - 1));

                if(shipToHit().get(0).getRow() > 0 && getGuessBoard()[shipToHit().get(0).getRow() - 1][shipToHit().get(0).getCol()] == 0)
                    return ATKSequence(enemy, new Location(shipToHit().get(0).getRow() - 1,shipToHit().get(0).getCol()));

                if(shipToHit().get(0).getCol() < 9 && getGuessBoard()[shipToHit().get(0).getRow()][shipToHit().get(0).getCol() + 1] == 0)
                    return ATKSequence(enemy, new Location(shipToHit().get(0).getRow(),shipToHit().get(0).getCol() + 1));
            }
            else
            {
//                System.out.println("ship is " + shipToHit().size() + " long");
                if(lookRow(shipToHit()))
                {
//                    System.out.println("row");
                    if(maxOrMinCol(shipToHit(),1) < 9)
                        if(getGuessBoard()[shipToHit().get(0).getRow()][maxOrMinCol(shipToHit(),1) + 1] == 0)
                            return ATKSequence(enemy,new Location(shipToHit().get(0).getRow(),maxOrMinCol(shipToHit(),1) + 1));
//                    System.out.println("ops");
                    return ATKSequence(enemy,new Location(shipToHit().get(0).getRow(),maxOrMinCol(shipToHit(),-1) -1));
                }
                else
                {
//                    System.out.println("col");
                    if(maxOrMinRow(shipToHit(),1) < 9)
                        if(getGuessBoard()[maxOrMinRow(shipToHit(),1) + 1][shipToHit().get(0).getCol()] == 0)
                            return ATKSequence(enemy,new Location(maxOrMinRow(shipToHit(),1) + 1,shipToHit().get(0).getCol()));
//                    System.out.println("ops");
                    return ATKSequence(enemy,new Location(maxOrMinRow(shipToHit(),-1) -1,shipToHit().get(0).getCol()));
                }
            }
        }

        // if there is a duplicate maximum location
        if(mappa.dupe() > 1)
        {
            if(mappa.maxVal() == 0.0)
            {
//                System.out.println(mappa.dupe() + " is dupe");
//                System.out.println(mappa.maxVal() + " is the value of dupe");
            }
            int row = ran.nextInt(10);
            int col = ran.nextInt(10);

            while(!(mappa.getMap()[row][col] == mappa.maxVal() && super.getGuessBoard()[row][col] == 0))
            {
                row = ran.nextInt(10);
                col = ran.nextInt(10);
//                System.out.println(row+ " " + col);
            }
            //Location L = new Location (row,col);
            Location L = mappa.dupeList()[mappa.dupeList().length/2 -1];

            return ATKSequence(enemy, L);
        }
        else
        {
            //System.out.println("hit best spot " + mappa.maxVal());
            return ATKSequence(enemy,mappa.max());
        }
    }

    /**
     * Construct an instance of
     *
     *   AircraftCarrier,
     *   Destroyer,
     *   Submarine,
     *   Cruiser, and
     *   PatrolBoat
     *
     * and add them to this Player's list of ships.
     */
    @Override
    public void populateShips()
    {
        load();

        hitsMap.placeShip();

        addShip(new AircraftCarrier(hitsMap.placeAC()));
        addShip(new Destroyer(hitsMap.placeDE()));
        addShip(new Cruiser(hitsMap.placeCR()));
        addShip(new Submarine(hitsMap.placeSU()));
        addShip(new PatrolBoat(hitsMap.placePB()));
    }

    @Override
    public boolean hasShipAtLocation(Location loc)
    {
        boolean rtn = super.hasShipAtLocation(loc);
        this.hitsMap.whenHit(loc, rtn, countGames);
        save();
        return rtn;
    }

    public static class HeatMap
    {
        private boolean AC = false, DE = false, SU = false, CR = false, PB = false;
        private double[][] map;
        private int [][] ac, de, su, cr, pb;
        private int[][] tuneAC, tuneDE, tuneSU, tuneCR, tunePB;
        private int[][] missMap;

        HeatMap()
        {
            map = new double[10][10];
            ac = new int[10][10];
            de = new int[10][10];
            su = new int[10][10];
            cr = new int[10][10];
            pb = new int[10][10];
            tuneAC = new int[10][10];
            tuneDE = new int[10][10];
            tuneSU = new int[10][10];
            tuneCR = new int[10][10];
            tunePB = new int[10][10];
            missMap = new int[10][10];
            makeMaps(tuneAC);
            makeMaps(tuneDE);
            makeMaps(tuneSU);
            makeMaps(tuneCR);
            makeMaps(tunePB);
            makeMaps(missMap);
        }

        void makeMaps(int[][] in)
        {
            for (int r = 0; r < in.length; r++)
                for (int c = 0; c < in.length; c++)
                    in[r][c] = 1;
        }


        HeatMap(int[][] tuneAC, int[][] tuneDE, int[][] tuneSU, int[][] tuneCR, int[][] tunePB, int[][] missMap)
        {
            map = new double[10][10];
            ac = new int[10][10];
            de = new int[10][10];
            su = new int[10][10];
            cr = new int[10][10];
            pb = new int[10][10];
            this.tuneAC = tuneAC;
            this.tuneDE = tuneDE;
            this.tuneSU = tuneSU;
            this.tuneCR = tuneCR;
            this.tunePB = tunePB;
            this.missMap = missMap;
        }

        public int[][] getTuneAC() {return tuneAC;}

        public int[][] getTuneDE() {return tuneDE;}

        public int[][] getTuneSU() {return tuneSU;}

        public int[][] getTuneCR() {return tuneCR;}

        public int[][] getTunePB() {return tunePB;}

        public int[][] getMissMap() {return missMap;}

        double[][] getMap() {return map;}

        void check()
        {
            checkOpenAC();
            checkOpenDE();
            checkOpenCR();
            checkOpenSU();
            checkOpenPB();
        }


        /**
         * replaces the location to -1 in all of the heat maps
         * @param l location of the miss
         */
        void placeMiss(Location l, boolean hit)
        {
            map[l.getRow()][l.getCol()] = -1;
            ac[l.getRow()][l.getCol()] = -1;
            de[l.getRow()][l.getCol()] = -1;
            su[l.getRow()][l.getCol()] = -1;
            cr[l.getRow()][l.getCol()] = -1;
            pb[l.getRow()][l.getCol()] = -1;
            check();

            if(!hit)
                missMap[l.getRow()][l.getCol()] ++;
        }

        /**
         * combines all Heatmaps of non-sunken enemy ships.
         */
        void addHM(int games)
        {
//            System.out.println("AC:" + AC + " DE:" + DE + " CR:" + CR + " SU:" + SU + " PB:" + PB);
            for (int r = 0; r < map.length; r++)
                for (int c = 0; c < map.length; c++)
                    if (map[r][c] != -1)
                    {
                        map[r][c] = 0;
                        if(!AC)
                            map[r][c] += (ac[r][c]) * (tuneAC[r][c]) / (double) (games);
                        if(!DE)
                            map[r][c] += (de[r][c]) * (tuneDE[r][c]) / (double) (games);
                        if(!CR)
                            map[r][c] += (cr[r][c]) * (tuneCR[r][c]) / (double) (games);
                        if(!SU)
                            map[r][c] += (su[r][c]) * (tuneSU[r][c]) / (double) (games);
                        if(!PB)
                            map[r][c] += (pb[r][c]) * (tunePB[r][c]) / (double) (games);

                        map[r][c] = map[r][c] / (double) (missMap[r][c] + 1); // / (arrMaxVal(missMap) +1)) + 1);
                    }
        }


        Location min()
        {
            int row = 0;
            int col = 0;
            for (int r = 0; r < map.length; r++)
                for (int c = 0; c < map.length; c++)
                    if(map[r][c] < map[row][col])
                    {
                        row = r;
                        col = c;
                    }
            return new Location(row,col);
        }

        /**
         * @return a Location list of all of the points with the highest heat level
         */
        Location[] dupeList()
        {
            Location[] rtn = new Location[dupe()];
            int count = 0;
            for (int r = 0; r < map.length; r++)
                for (int c = 0; c < map.length; c++)
                    if(map[r][c] == map[max().getRow()][max().getCol()])
                    {
                        rtn[count] = new Location(r, c);
                        count ++;
                    }
            return rtn;
        }

        /**
         * @return the last traversed location with the highest heat level
         */
        Location max()
        {
            int row = 0;
            int col = 0;
            for (int r = 0; r < map.length; r++)
                for (int c = 0; c < map.length; c++)
                    if(map[r][c] > map[row][col])
                    {
                        row = r;
                        col = c;
                    }
            return new Location(row,col);
        }

        /**
         * @return the highest heat level of the map
         */
        double maxVal()
        {
            double rtn = 0;
            for (int r = 0; r < map.length; r++)
                for (int c = 0; c < map.length; c++)
                    if(map[r][c] > rtn)
                        rtn = map[r][c];
            return rtn;
        }

        int arrMaxVal(int[][] arr)
        {
            int rtn = 0;
            for (int r = 0; r < arr.length; r++)
                for (int c = 0; c < arr.length; c++)
                    if(arr[r][c] > rtn)
                        rtn = arr[r][c];
            return rtn;
        }

        /**
         * @return the number of slots of highest heat.
         */
        int dupe()
        {
            int rtn =0;
            for (int r = 0; r < map.length; r++)
                for (int c = 0; c < map.length; c++)
                    if(map[r][c] == map[max().getRow()][max().getCol()])
                        rtn ++;
            return rtn;
        }

        /**
         * sets the entire board to 0
         * @param i board
         */
        void clear(int[][] i)
        {
            for (int r = 0; r < i.length; r++)
                for (int c = 0; c < i.length; c++)
                    if(i[r][c] != -1)
                        i[r][c] = 0;
        }

        /*
        ---------------------------------------------------------------------------------------------------------------
        ALL  OF THE CHECK OPENS WILL DO THE PROBABILITY MAPPING OF THE SHIP'S HEAT
        ---------------------------------------------------------------------------------------------------------------
         */

        void checkOpen(int[][] arr , int len)
        {
            clear(arr);
            for (int r = 0; r < arr.length; r++)
                for (int c = 0; c < arr.length - len; c++)
                {
                    int count = 0;
                    for (int i = 0; i < len; i++)
                        if (ac[r][c + i] < 0)
                            count++;
                    if(count == 0)
                        for (int i = 0; i < len; i++)
                            ac[r][c + i]++;
                }


            for (int c = 0; c < arr.length; c++)
                for (int r = 0; r < arr.length - len; r++)
                {
                    int count = 0;
                    for (int i = 0; i < len; i++)
                        if (ac[r + i][c] < 0)
                            count++;
                    if(count == 0)
                        for (int i = 0; i < len; i++)
                            ac[r + i][c]++;
                }
        }


        void checkOpenAC()
        {
            clear(ac);
            for (int r = 0; r < ac.length; r++)
                for (int c = 0; c < ac.length - 5; c++)
                    if (ac[r][c] != -1 && ac[r][c + 1] != -1 && ac[r][c + 2] != -1 && ac[r][c + 3] != -1 && ac[r][c + 4] != -1)
                    {
                        ac[r][c] += 1;
                        ac[r][c + 1] += 1;
                        ac[r][c + 2] += 1;
                        ac[r][c + 3] += 1;
                        ac[r][c + 4] += 1;

                    }
            for (int c = 0; c < ac.length; c++)
                for (int r = 0; r < ac.length - 5; r++)
                    if(ac[r][c] != -1 && ac[r + 1][c] != -1 && ac[r + 2][c] != -1 && ac[r + 3][c] != -1 && ac[r + 4][c] != -1)
                    {
                        ac[r][c] += 1;
                        ac[r + 1][c] += 1;
                        ac[r + 2][c] += 1;
                        ac[r + 3][c] += 1;
                        ac[r + 4][c] += 1;
                    }
        }

        void checkOpenDE()
        {
            clear(de);
            for (int r = 0; r < de.length; r++)
                for (int c = 0; c < de.length - 4; c++)
                    if (de[r][c] != -1 && de[r][c + 1] != -1 && de[r][c + 2] != -1 && de[r][c + 3] != -1)
                    {
                        de[r][c] += 1;
                        de[r][c + 1] += 1;
                        de[r][c + 2] += 1;
                        de[r][c + 3] += 1;
                    }
            for (int c = 0; c < de.length; c++)
                for (int r = 0; r < de.length - 4; r++)
                    if(de[r][c] != -1 && de[r + 1][c] != -1 && de[r + 2][c] != -1 && de[r + 3][c] != -1)
                    {
                        de[r][c] += 1;
                        de[r + 1][c] += 1;
                        de[r + 2][c] += 1;
                        de[r + 3][c] += 1;
                    }
        }

        void checkOpenSU()
        {
            clear(su);
            for (int r = 0; r < su.length; r++)
                for (int c = 0; c < su.length - 3; c++)
                    if (su[r][c] != -1 && su[r][c + 1] != -1 && su[r][c + 2] != -1)
                    {
                        su[r][c] += 1;
                        su[r][c + 1] += 1;
                        su[r][c + 2] += 1;
                    }
            for (int c = 0; c < su.length; c++)
                for (int r = 0; r < su.length - 3; r++)
                    if(su[r][c] != -1 && su[r + 1][c] != -1 && su[r + 2][c] != -1)
                    {
                        su[r][c] += 1;
                        su[r + 1][c] += 1;
                        su[r + 2][c] += 1;
                    }
        }

        void checkOpenCR()
        {
            clear(cr);
            for (int r = 0; r < cr.length; r++)
                for (int c = 0; c < cr.length - 3; c++)
                    if (cr[r][c] != -1 && cr[r][c + 1] != -1 && cr[r][c + 2] != -1)
                    {
                        cr[r][c] += 1;
                        cr[r][c + 1] += 1;
                        cr[r][c + 2] += 1;
                    }
            for (int c = 0; c < cr.length; c++)
                for (int r = 0; r < cr.length - 3; r++)
                    if(cr[r][c] != -1 && cr[r + 1][c] != -1 && cr[r + 2][c] != -1)
                    {
                        cr[r][c] += 1;
                        cr[r + 1][c] += 1;
                        cr[r + 2][c] += 1;
                    }
        }

        void checkOpenPB()
        {
            clear(pb);
            for (int r = 0; r < pb.length; r++)
                for (int c = 0; c < pb.length - 2; c++)
                    if (pb[r][c] != -1 && pb[r][c + 1] != -1)
                    {
                        pb[r][c] += 1;
                        pb[r][c + 1] += 1;
                    }
            for (int c = 0; c < pb.length; c++)
                for (int r = 0; r < pb.length - 2; r++)
                    if(pb[r][c] != -1 && pb[r + 1][c] != -1)
                    {
                        pb[r][c] += 1;
                        pb[r + 1][c] += 1;
                    }
        }

        void setAC(boolean AC) {this.AC = AC;}
        void setDE(boolean DE) {this.DE = DE;}
        void setSU(boolean SU) {this.SU = SU;}
        void setCR(boolean CR) {this.CR = CR;}
        void setPB(boolean PB) {this.PB = PB;}
    }



    private static class HitsMap
    {
        int numOfHits;
        int numOfAtk;
        int[][] hitsTaken = new int[10][10];

        HitsMap()
        {
            numOfHits = 0;
            numOfAtk = 0;
            for (int r = 0; r < hitsTaken.length; r++)
                for (int c = 0; c < hitsTaken.length; c++)
                    hitsTaken[r][c] = 0;
        }

        HitsMap(int[][] hitsTaken) {this.hitsTaken = hitsTaken;}

        public int[][] getHitsTaken() {return hitsTaken;}

        void whenHit(Location loc, boolean hit, int gameCount)
        {
            if(hit)
                numOfHits ++;
            numOfAtk ++;
            int paramOne = (Math.max(1, 10 - gameCount));
            int paramTwo = ((int) Math.sqrt(100 * Math.max(100 - numOfAtk, 1)));
            hitsTaken[loc.getRow()][loc.getCol()] += paramOne * paramTwo;
        }

        boolean contains(Location[] locList, int r, int c)
        {
            for (Location loc : locList)
            {
                if (loc.getRow() == r && loc.getCol() == c)
                    return true;
            }
            return false;
        }

        Location[] positionAC = placeAC();
        Location[] positionDE = placeDE();
        Location[] positionSU = placeSU();
        Location[] positionCR = placeCR();
        Location[] positionPB = placePB();

        void placeShip()
        {
            Location[] positionAC = placeAC();
            Location[] positionDE = placeDE();
            Location[] positionSU = placeSU();
            Location[] positionCR = placeCR();
            Location[] positionPB = placePB();
        }

        public Location[] getPositionAC()
        {
            return positionAC;
        }

        public Location[] getPositionDE()
        {
            return positionDE;
        }

        public Location[] getPositionSU()
        {
            return positionSU;
        }

        public Location[] getPositionCR()
        {
            return positionCR;
        }

        public Location[] getPositionPB()
        {
            return positionPB;
        }

        Location[] placeAC()
        {
            int current = 0;
            int bestscore = -1;
            Location[] best = new Location[5];
            for (int r = 0; r < 10; r++)
            {
                for (int c = 0; c < 10 - 5; c++)
                {
                    current += hitsTaken[r][c];
                    current += hitsTaken[r][c + 1];
                    current += hitsTaken[r][c + 2];
                    current += hitsTaken[r][c + 3];
                    current += hitsTaken[r][c + 4];

                    if(current < bestscore || bestscore < 0)
                    {
                        bestscore = current;
                        best[0] = new Location(r, c);
                        best[1] = new Location(r, c + 1);
                        best[2] = new Location(r, c + 2);
                        best[3] = new Location(r, c + 3);
                        best[4] = new Location(r, c + 4);
//                        System.out.println(bestscore);
//                        System.out.println(best[0].getRow() + " " + best[0].getCol());
                    }
                    current = 0;

                }
            }
            for (int c = 0; c < 10; c++)
            {
                for (int r = 0; r < 10 - 5; r++)
                {
                    current += hitsTaken[r][c];
                    current += hitsTaken[r + 1][c];
                    current += hitsTaken[r + 2][c];
                    current += hitsTaken[r + 3][c];
                    current += hitsTaken[r + 4][c];

                    if(current < bestscore || bestscore < 0)
                    {
                        bestscore = current;
                        best[0] = new Location(r, c);
                        best[1] = new Location(r + 1, c);
                        best[2] = new Location(r + 2, c);
                        best[3] = new Location(r + 3, c);
                        best[4] = new Location(r + 4, c);
                    }
                    current = 0;
                }
            }
            positionAC = best;
            return best;
        }

        Location[] placeDE()
        {
            int current = 0;
            int bestscore = -1;
            Location[] best = new Location[4];
            for (int r = 0; r < 10; r++)
            {
                for (int c = 0; c < 10 - 4; c++)
                {
                    current += hitsTaken[r][c];
                    if(contains(positionAC, r, c)) break;

                    current += hitsTaken[r][c + 1];
                    if(contains(positionAC, r, c + 1)) break;

                    current += hitsTaken[r][c + 2];
                    if(contains(positionAC, r, c + 2)) break;

                    current += hitsTaken[r][c + 3];
                    if(contains(positionAC, r, c + 3)) break;

                    if(current < bestscore || bestscore < 0)
                    {
                        bestscore = current;
                        best[0] = new Location(r, c);
                        best[1] = new Location(r, c + 1);
                        best[2] = new Location(r, c + 2);
                        best[3] = new Location(r, c + 3);
                    }
                    current = 0;
                }
            }
            for (int c = 0; c < 10; c++)
            {
                for (int r = 0; r < 10 - 4; r++)
                {

                    current += hitsTaken[r][c];
                    if(contains(positionAC, r, c)) break;

                    current += hitsTaken[r + 1][c];
                    if(contains(positionAC, r + 1, c)) break;

                    current += hitsTaken[r + 2][c];
                    if(contains(positionAC, r + 2, c)) break;

                    current += hitsTaken[r + 3][c];
                    if(contains(positionAC, r + 3, c)) break;

                    if(current < bestscore || bestscore < 0)
                    {
                        bestscore = current;
                        best[0] = new Location(r, c);
                        best[1] = new Location(r + 1, c);
                        best[2] = new Location(r + 2, c);
                        best[3] = new Location(r + 3, c);
                    }
                    current = 0;
                }
            }
            positionDE = best;
            return best;
        }

        Location[] placeSU()
        {
            int current = 0;
            int bestscore = -1;
            Location[] best = new Location[3];
            for (int r = 0; r < 10; r++)
            {
                for (int c = 0; c < 10 - 3; c++)
                {
                    current += hitsTaken[r][c];
                    if(contains(positionAC, r, c) || contains(positionDE, r, c)) break;

                    current += hitsTaken[r][c + 1];
                    if(contains(positionAC, r, c + 1) || contains(positionDE, r, c + 1)) break;

                    current += hitsTaken[r][c + 2];
                    if(contains(positionAC, r, c + 2) || contains(positionDE, r, c + 2)) break;

                    if(current < bestscore || bestscore < 0)
                    {
                        bestscore = current;
                        best[0] = new Location(r, c);
                        best[1] = new Location(r, c + 1);
                        best[2] = new Location(r, c + 2);
                    }
                    current = 0;
                }
            }
            for (int c = 0; c < 10; c++)
            {
                for (int r = 0; r < 10 - 3; r++)
                {
                    current += hitsTaken[r][c];
                    if(contains(positionAC, r, c) || contains(positionDE, r, c)) break;

                    current += hitsTaken[r + 1][c];
                    if(contains(positionAC, r + 1, c) || contains(positionDE, r + 1, c)) break;

                    current += hitsTaken[r + 2][c];
                    if(contains(positionAC, r + 2, c) || contains(positionDE, r + 2, c)) break;

                    if(current < bestscore || bestscore < 0)
                    {
                        bestscore = current;
                        best[0] = new Location(r, c);
                        best[1] = new Location(r + 1, c);
                        best[2] = new Location(r + 2, c);
                    }
                    current = 0;
                }
            }
            positionSU = best;
            return best;
        }

        Location[] placeCR()
        {
            int current = 0;
            int bestscore = -1;
            Location[] best = new Location[3];
            for (int r = 0; r < 10; r++)
            {
                for (int c = 0; c < 10 - 3; c++)
                {
                    current += hitsTaken[r][c];
                    if(contains(positionAC, r, c) || contains(positionDE, r, c) || contains(positionSU, r, c)) break;

                    current += hitsTaken[r][c + 1];
                    if(contains(positionAC, r, c + 1) || contains(positionDE, r, c + 1) || contains(positionSU, r, c + 1)) break;

                    current += hitsTaken[r][c + 2];
                    if(contains(positionAC, r, c + 2) || contains(positionDE, r, c + 2) || contains(positionSU, r, c + 2)) break;

                    if(current < bestscore || bestscore < 0)
                    {
                        bestscore = current;
                        best[0] = new Location(r, c);
                        best[1] = new Location(r, c + 1);
                        best[2] = new Location(r, c + 2);
                    }
                    current = 0;
                }
            }
            for (int c = 0; c < 10; c++)
            {
                for (int r = 0; r < 10 - 3; r++)
                {
                    current += hitsTaken[r][c];
                    if(contains(positionAC, r, c) || contains(positionDE, r, c) || contains(positionSU, r, c)) break;

                    current += hitsTaken[r + 1][c];
                    if(contains(positionAC, r + 1, c) || contains(positionDE, r + 1, c) || contains(positionSU, r + 1, c)) break;

                    current += hitsTaken[r + 2][c];
                    if(contains(positionAC, r + 2, c) || contains(positionDE, r + 2, c) || contains(positionSU, r + 2, c)) break;

                    if(current < bestscore || bestscore < 0)
                    {
                        bestscore = current;
                        best[0] = new Location(r, c);
                        best[1] = new Location(r + 1, c);
                        best[2] = new Location(r + 2, c);
                    }
                    current = 0;
                }
            }
            positionCR = best;
            return best;
        }

        Location[] placePB()
        {
            int current = 0;
            int bestscore = -1;
            Location[] best = new Location[2];
            for (int r = 0; r < 10; r++)
            {
                for (int c = 0; c < 10 - 2; c++)
                {
                    current += hitsTaken[r][c];
                    if(contains(positionAC, r, c) || contains(positionDE, r, c) || contains(positionSU, r, c) || contains(positionCR, r, c)) break;

                    current += hitsTaken[r][c + 1];
                    if(contains(positionAC, r, c + 1) || contains(positionDE, r, c + 1) || contains(positionSU, r, c + 1) || contains(positionCR, r, c + 1)) break;

                    if(current < bestscore || bestscore < 0)
                    {
                        bestscore = current;
                        best[0] = new Location(r, c);
                        best[1] = new Location(r, c + 1);
                    }
                    current = 0;
                }
            }
            for (int c = 0; c < 10; c++)
            {
                for (int r = 0; r < 10 - 2; r++)
                {
                    current += hitsTaken[r][c];
                    if(contains(positionAC, r, c) || contains(positionDE, r, c) || contains(positionSU, r, c) || contains(positionCR, r, c)) break;

                    current += hitsTaken[r + 1][c];
                    if(contains(positionAC, r + 1, c) || contains(positionDE, r + 1, c) || contains(positionSU, r + 1, c) || contains(positionCR, r + 1, c)) break;

                    if(current < bestscore || bestscore < 0)
                    {
                        bestscore = current;
                        best[0] = new Location(r, c);
                        best[1] = new Location(r + 1, c);
                    }
                    current = 0;
                }
            }
            positionPB = best;
            return best;
        }

    }

    private void add(int[][]base, int[][] addon)
    {
        for (int r = 0; r < base.length; r++)
            for (int c = 0; c < base.length; c++)
                base[r][c] += addon[r][c];
    }

    private void save() {
        File file = stateFile;

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(stringMaker());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load()
    {
        File file = stateFile;

        if(!file.exists())
        {
            this.enemyName = Double.toString(Math.random());
            this.countGames = 1;
            this.mappa = new HeatMap();
            this.hitsMap = new HitsMap();

            this.save();
            return;
        }

        //when file does exist ask for the file
        try(Scanner text = new Scanner(file))
        {
            this.enemyName = text.nextLine();
            this.countGames = Integer.parseInt(text.nextLine()) +1; //take last played game number and add 1 for this game

            int[][] gridAC = new int[10][10];
            for (int i = 0; i < 10; i++)
            {
                String[] row = text.nextLine().split(" ");

                for (int j = 0; j < 10; j++)
                {
                    gridAC[i][j] = Integer.parseInt(row[j]);
                }
            }

            int[][] gridDE = new int[10][10];
            for (int i = 0; i < 10; i++)
            {
                String[] row = text.nextLine().split(" ");

                for (int j = 0; j < 10; j++)
                {
                    gridDE[i][j] = Integer.parseInt(row[j]);
                }
            }

            int[][] gridSU = new int[10][10];
            for (int i = 0; i < 10; i++)
            {
                String[] row = text.nextLine().split(" ");

                for (int j = 0; j < 10; j++)
                {
                    gridSU[i][j] = Integer.parseInt(row[j]);
                }
            }

            int[][] gridCR = new int[10][10];
            for (int i = 0; i < 10; i++)
            {
                String[] row = text.nextLine().split(" ");

                for (int j = 0; j < 10; j++)
                {
                    gridCR[i][j] = Integer.parseInt(row[j]);
                }
            }

            int[][] gridPB = new int[10][10];
            for (int i = 0; i < 10; i++)
            {
                String[] row = text.nextLine().split(" ");

                for (int j = 0; j < 10; j++)
                {
                    gridPB[i][j] = Integer.parseInt(row[j]);
                }
            }

            int[][] gridMiss = new int[10][10];
            for (int i = 0; i < 10; i++)
            {
                String[] row = text.nextLine().split(" ");

                for (int j = 0; j < 10; j++)
                {
                    gridMiss[i][j] = Integer.parseInt(row[j]);
                }
            }

            this.mappa = new HeatMap(gridAC, gridDE, gridSU, gridCR, gridPB, gridMiss);

            int[][] hitMap = new int[10][10];
            for (int i = 0; i < 10; i++)
            {
                String[] row = text.nextLine().split(" ");
                for (int j = 0; j < 10; j++)
                {
                    hitMap[i][j] = Integer.parseInt(row[j]);
                }
            }

            this.hitsMap = new HitsMap(hitMap);
        }
        catch (FileNotFoundException error)         // if the retrieving fails.
        {
            error.printStackTrace();
        }
    }

    private int[][] shipToGrid(ArrayList<Location> ship)
    {
        int[][] rtn = new int[10][10];
        for (Location l: ship)
            rtn[l.getRow()][l.getCol()] = 1;
        return rtn;
    }

    private static StringBuilder gridToText(int[][] grid) {
        StringBuilder output = new StringBuilder();

        for (int[] row : grid)
        {
            for (int col : row)
            {
                output.append(col).append(" ");
            }

            output.append("\n");
        }
        return output;
    }

    private String stringMaker() {
        StringBuilder output = new StringBuilder();

        output.append(this.enemyName).append("\n");
        output.append(this.countGames).append("\n");

        output.append(gridToText(this.mappa.getTuneAC()));
        output.append(gridToText(this.mappa.getTuneDE()));
        output.append(gridToText(this.mappa.getTuneSU()));
        output.append(gridToText(this.mappa.getTuneCR()));
        output.append(gridToText(this.mappa.getTunePB()));
        output.append(gridToText(this.mappa.getMissMap()));
        output.append(gridToText(this.hitsMap.getHitsTaken()));
        return output.substring(0, output.length() - 1);
    }

}