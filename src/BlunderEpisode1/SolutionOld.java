package BlunderEpisode1;

import java.util.*;
import java.io.*;
import java.math.*;

public class SolutionOld {

    /**
     * Auto-generated code below aims at helping you parse
     * the standard input according to the problem statement.
     **/

    /*
     * Todo: Find the 2 T's.
     * Add statetracking
     */

    public static void main(String args[]) {
        boolean breakerMode = false;
        boolean invertedDirections = false;
        boolean onStreak = true;

        String[] dirText = {"SOUTH", "EAST", "NORTH", "WEST"}; //moves in strings
        int[] dirX = {0, 1, 0 , -1}; //moves translated in x
        int[] dirY = {1, 0, -1 , 0}; //moves translated in y
        int bX = -1; int bY = -1; //position of bender
        int M = 4; //available moves
        int nT = 2; int ti = 0; //indextracker and expected amount of teleporters
        int[] tX = new int[nT];
        int[] tY = new int[nT];
        int lastDirection = 0; //the last used direction index which will repeat until obstacle


        Scanner in = new Scanner(System.in);
        int L = in.nextInt();
        int C = in.nextInt();
        in.nextLine();
        char[][] grid = new char[L][C];

        for (int i = 0; i < L; i++) {
            String line = in.nextLine();
            grid[i] = line.toCharArray();
            if(line.contains("@")){
                bX = line.indexOf("@");
                bY = i;
            }
            if(line.contains("T")){
                tX[ti]= line.indexOf("T");
                tY[ti++] = i;
            }
        }
        String nextMove = "";
        //map:
        for(int i = 0; i < L; ++i){
            System.err.println(grid[i]);
        }
        System.err.println("teleporters at (" + tX[0] + "," + tY[0] + ") and (" + tX[1] + "," + tY[1] + ")");
        System.err.println("bender at: " +bX + " " + bY);

        while(true){
            /*
             * Find next move
             * Perform next move
             * update any special effects
             *
             */
            int direction = lastDirection;
            char map;
            onStreak = true;

            for(int i  = 0; i < M+1; ++i){
                map = grid[bY + dirY[direction]][bX + dirX[direction]]; //Get status of next prefered move

                //only move if allowed
                if(!( map == '#' || ( !breakerMode && map == 'X'))){
                    nextMove = dirText[direction];
                    bX += dirX[direction];
                    bY += dirY[direction];

                    if(breakerMode && map == 'X'){
                        grid[bY][bX] = ' '; //remove obstacle
                    }


                    switch(map){
                        case 'S':
                            direction = 0;
                            break;
                        case 'E':
                            direction = 1;
                            break;
                        case 'N':
                            direction = 2;
                            break;
                        case 'W':
                            direction = 3;
                            break;
                        case 'B':
                            breakerMode = !breakerMode;
                            break;
                        case 'I':
                            invertedDirections = !invertedDirections;
                            break;
                        case 'T':
                            if(bX == tX[0] && bY == tY[0]){
                                bX = tX[1];
                                bY = tY[1];
                            }
                            else{
                                bX = tX[0];
                                bY = tY[0];
                            }
                            break;
                        default:
                            break;
                    }
                    lastDirection = direction;
                    break;
                }
                else{
                    if(onStreak){
                        direction = (invertedDirections) ? M-1: 0;
                        onStreak = false;
                    }
                    else if(invertedDirections){
                        direction = (direction + M-1) % M; // -> 3 2 1 0
                    }
                    else{
                        direction = (direction +1) % M; // -> 0 1 2 3
                    }
                }
            }

            System.out.println(nextMove);
//            System.out.println("LOOP");

            //When bender has reached the goal (and move to goal has been printed)
            if( grid[bY][bX] == '$'){
                break;
            }
            break;
        }

//        System.err.println("nextMove: " + nextMove);

        // Write an action using System.out.println()
        // To debug: System.err.println("Debug messages...");


    }
}

