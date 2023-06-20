import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class MoveFiles
{
    public static void main(String[] args) throws IOException
    {
        if (args.length < 2)
            return;

        File original = new File(args[0]);
        File copied = new File(args[1]);

        FileUtils.copyFile(original, copied);
    }
}
