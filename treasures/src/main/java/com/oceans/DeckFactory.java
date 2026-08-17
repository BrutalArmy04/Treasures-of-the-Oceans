package com.oceans;

import java.util.ArrayList;
import java.util.Collections;


public class DeckFactory
{

    private static ArrayList<Card> masterList = new ArrayList<>(); 

    private static void initializeCards() {
        masterList.clear(); 

        masterList.add(new Card("Great White Shark", 58, 89, 96));
        masterList.add(new Card("Whale Shark", 18, 100, 4)); // 100 Size limit
        masterList.add(new Card("Hammerhead Shark", 62, 83, 79));
        masterList.add(new Card("Giant Squid", 71, 94, 88));
        masterList.add(new Card("Sailfish", 100, 58, 29)); // 100 Speed limit
        masterList.add(new Card("Black Marlin", 96, 74, 42));
        masterList.add(new Card("Swordfish", 88, 69, 67));
        masterList.add(new Card("Barracuda", 86, 41, 77));
        masterList.add(new Card("Pufferfish", 23, 16, 84));
        masterList.add(new Card("Piranha", 67, 12, 91));
        masterList.add(new Card("Lionfish", 28, 21, 86));
        masterList.add(new Card("Blue-Ringed Octopus", 39, 4, 100)); // 100 Danger limit
        masterList.add(new Card("Electric Eel", 46, 52, 93));
        masterList.add(new Card("Moray Eel", 51, 47, 73));
        masterList.add(new Card("Manta Ray", 38, 86, 14));
        masterList.add(new Card("Tuna", 76, 53, 19));
        masterList.add(new Card("Clownfish", 43, 9, 6));
        masterList.add(new Card("Seahorse", 14, 6, 3));
        masterList.add(new Card("Flounder", 36, 26, 11));
        masterList.add(new Card("Blobfish", 4, 31, 2));
        masterList.add(new Card("Orca (Killer Whale)", 64, 87, 97));
        masterList.add(new Card("Blue Whale", 33, 99, 3)); 
        masterList.add(new Card("Sperm Whale", 41, 96, 76));
        masterList.add(new Card("Bottlenose Dolphin", 61, 42, 18));
        masterList.add(new Card("Narwhal", 49, 56, 44));
        masterList.add(new Card("Walrus", 29, 67, 61));
        masterList.add(new Card("Leopard Seal", 47, 49, 81));
        masterList.add(new Card("Manatee", 16, 62, 2));
        masterList.add(new Card("Sea Otter", 21, 14, 9));
        masterList.add(new Card("Tiger Shark", 56, 73, 91));
        masterList.add(new Card("Bull Shark", 46, 64, 93));
        masterList.add(new Card("Shortfin Mako Shark", 97, 54, 82)); 
        masterList.add(new Card("Greenland Shark", 14, 81, 38));
        masterList.add(new Card("Goblin Shark", 24, 46, 58));
        masterList.add(new Card("Stingray", 32, 34, 69));
        masterList.add(new Card("Sawfish", 36, 59, 84));
        masterList.add(new Card("Colossal Squid", 39, 92, 87));
        masterList.add(new Card("Vampire Squid", 23, 11, 16));
        masterList.add(new Card("Anglerfish", 16, 9, 51));
        masterList.add(new Card("Gulper Eel", 19, 16, 41));
        masterList.add(new Card("Giant Isopod", 11, 4, 6));
        masterList.add(new Card("Oarfish", 21, 72, 4));
        masterList.add(new Card("Box Jellyfish", 9, 6, 99)); 
        masterList.add(new Card("Portuguese Man o' War", 4, 16, 86));
        masterList.add(new Card("Stonefish", 6, 9, 96));
        masterList.add(new Card("Cone Snail", 2, 3, 97)); 
        masterList.add(new Card("Belcher's Sea Snake", 26, 11, 98));
        masterList.add(new Card("Crown-of-Thorns Starfish", 4, 14, 76));
        masterList.add(new Card("Saltwater Crocodile", 34, 71, 98));
        masterList.add(new Card("Leatherback Sea Turtle", 31, 64, 14));
        masterList.add(new Card("Marine Iguana", 19, 11, 6));
        masterList.add(new Card("Ocean Sunfish", 11, 77, 2));
        masterList.add(new Card("Goliath Grouper", 26, 66, 49));
        masterList.add(new Card("Tarpon", 51, 39, 21));
        masterList.add(new Card("Mahi-Mahi", 69, 31, 14));
        masterList.add(new Card("Coelacanth", 14, 34, 11));
        masterList.add(new Card("Mantis Shrimp", 82, 4, 63)); 
        masterList.add(new Card("Japanese Spider Crab", 14, 51, 24));
        masterList.add(new Card("Giant Clam", 2, 42, 1));
        masterList.add(new Card("Cuttlefish", 37, 14, 21));
    }
    public static ArrayList<Card> returnDeck()
{
    initializeCards();
    Collections.shuffle(masterList);
    return masterList;
}

    }
