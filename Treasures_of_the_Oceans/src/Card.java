public class Card{
    private String name;
    private int id, speed, size, danger;

    private static int idCounter = 0 ;

    public Card(String name, int speed, int size, int danger)
    {
        this.id = idCounter++;
        this.speed = speed;
        this.size = size;
        this.danger = danger;
        this.name = name;
    }

    public String getName()
    {return this.name;}
    public int getSpeed()
    {return this.speed;}
     public int getSize()
    {return this.size;}
     public int getDanger()
    {return this.danger;}
    public int getId()
    {return id;}

    @Override
    public String toString()
    {
        return this.getName() + ": ID:" + this.getId() + ", " + this.getSpeed() + ", " + this.getSize() + ", " + this.getDanger();
    }
}