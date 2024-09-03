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

        if (args.length >= 3)
        {
            File[] files = copied.getParentFile().listFiles((dir, name) -> name.startsWith(args[2]));
            assert files != null;

            for (File file : files)
            {
                file.deleteOnExit();
            }
        }


        FileUtils.copyFile(original, copied);
        System.out.println(original.getAbsolutePath() + " was copied to " + copied.getAbsolutePath());
    }
}
