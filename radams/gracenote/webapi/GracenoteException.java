package radams.gracenote.webapi;

public class GracenoteException extends Exception
{
    private static final long serialVersionUID = 3279913248865272991L;
    private String _message = "";

    public GracenoteException(String message)
    {
        super();
        this._message = message;
    }

    public String getMessage() { return this._message; }
}
