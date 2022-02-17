package processor.server.graph_partition;

import it.unimi.dsi.fastutil.ints.IntSet;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import toools.collections.primitive.SelfAdaptiveIntSet;
import toools.extern.ExternalProgram;
import toools.extern.Proces;
import toools.io.file.Directory;
import toools.io.file.RegularFile;
import toools.net.NetUtilities;
import toools.text.TextUtilities;

import toools.log.Logger;
import toools.log.StdOutLogger;
import traffic.road.GridCell;

import java.io.*;
import java.util.*;

public class Metis {
    public final static RegularFile CMD = new RegularFile(new Directory("/usr/bin/"), "gpmetis");
    
    public static void ensureCompiled(Logger logger)
    {
        if (!CMD.exists())
        {
            try
            {
                ExternalProgram.ensureCommandsAreAvailable("gcc", "make", "cmake");
                Directory srcDir = CMD.getParent().getParent().getParent();
                RegularFile sourceArchive = new RegularFile(srcDir.getPath() + ".tar.gz");
                
                if (!sourceArchive.exists())
                {
                    logger.log("Downloading http://glaros.dtc.umn.edu/gkhome/fetch/sw/metis/metis-5.0.2.tar.gz");
                    sourceArchive.setContent(NetUtilities.retrieveURLContent("http://glaros.dtc.umn.edu/gkhome/fetch/sw/metis/metis-5.0.2.tar.gz"));
                }
                
                logger.log(srcDir.getPath());
                logger.log("Unarchiving");
                Proces.exec("tar", sourceArchive.getParent(), "xzf", "metis-5.0.2.tar.gz");
                logger.log("Configuring");
                
                logger.log("replaces the architecture-dependant target directory by one fixed");
                RegularFile makeFile = new RegularFile(srcDir, "Makefile");
                String makeText = new String(makeFile.getContent());
                makeText = makeText.replace("BUILDDIR = build/$(systype)-$(cputype)", "BUILDDIR = build");
                makeFile.setContent(makeText.getBytes());
                
                Proces.exec("make", srcDir, "config");
                logger.log("Compiling");
                Proces.exec("make", srcDir);
                logger.log("Done");
            }
            catch (IOException e)
            {
                throw new IllegalStateException(e);
            }
        }
    }
    
    public enum Ptype {
        rb, // Recursive bisectioning
        kway
        // Direct k-way partitioning [default]
    }
    
    public enum Ctype {
        rm, // Random matching
        shem
        // Sorted heavy-edge matching [default]
    }
    
    public enum Iptype {
        grow, // Grow a bisection using a greedy scheme [default for ncon=1]
        random
        // Compute a bisection at random [default for ncon>1]
    }
    
    public enum Objtype {
        cut, // Minimize the edgecut [default]
        vol
        // Minimize the total communication volume
    }
    
   

    public List<IntSet> compute(Graph<Integer, DefaultWeightedEdge> g, Map<GridCell, Integer> cellGraphIndex, int numberOfPartitions, Random r, Map<String, GridCell> workCellsSet)
    {
        return compute(g,cellGraphIndex, numberOfPartitions, Ptype.kway, Ctype.shem, Iptype.random, Objtype.vol, false, false, 30, 10, 1, r, workCellsSet);
    }
    
    public List<IntSet> compute(Graph<Integer, DefaultWeightedEdge> g, Map<GridCell, Integer> cellGraphIndex, int numberOfPartitions, Ptype ptype, Ctype ctype, Iptype iptype, Objtype objtype, boolean contig, boolean minconn,
                                int ufactor, int niter, int ncuts, Random r,Map<String, GridCell> workCellsSet)
    {
        ensureCompiled(StdOutLogger.SYNCHRONIZED_INSTANCE);
        
        // write the file that will be given to METIS
        RegularFile inFile = new RegularFile(Directory.getCurrentDirectory(), "metis-input-file");
        OutputStream os = inFile.createWritingStream();
        new MetisWriter().printGraph(g,cellGraphIndex, workCellsSet, os);
//        System.out.println(os);
//         Proces.TRACE_CALLS = true;
        // call the METIS process

        Collection<String> parms = new ArrayList<String>(Arrays.asList("-ptype=" + ptype.name(), "-ctype=" + ctype.name(), "-iptype=" + iptype.name(),
                "-objtype=" + objtype.name()));

        if (contig)
        {
            parms.add("-contig");
        }

        if (minconn)
        {
            parms.add("-minconn");
        }

        parms.addAll(Arrays.asList("-ufactor=" + ufactor, "-niter=" + niter, "-ncuts=" + ncuts, "-seed=" + r.nextInt(), inFile.getName(), ""
                + numberOfPartitions));

        byte[] stdout = Proces.exec(CMD.getPath(), inFile.getParent(), parms.toArray(new String[0]));

        inFile.delete();
//         System.out.println(new String(stdout));

        // retrieves the content from the file generated by METIS
        RegularFile outFile = new RegularFile(inFile.getParent(), inFile.getName() + ".part." + numberOfPartitions);
        // System.out.println(new String(outFile.getContent()));
        List<String> lines = TextUtilities.splitInLines(new String(outFile.getContent()));
        outFile.delete();

        // builds the partitionning
        List<IntSet> sets = createEmptySets(numberOfPartitions);

        for (int l = 0; l < lines.size(); ++l)
        {
            // in Metis, assignment for vertex 1 is at line 0
            int v = l + 1;

            // if the graph was relabelled, we need to restore the original
            // labelling
//            if (g != h)
//            {
//                --v;
//            }
            // System.out.println("line:" + lines.get(l) + "$");
            int setIndex = Integer.valueOf(lines.get(l));
            IntSet set = sets.get(setIndex);
            set.add(v);
        }

        return sets;
    }
    
  
    protected List<IntSet> createEmptySets(int numberOfPartitions)
    {
        List<IntSet> sets = new ArrayList<IntSet>();
        
        for (int i = 0; i < numberOfPartitions; ++i)
        {
            sets.add(new SelfAdaptiveIntSet(0));
        }
        
        return sets;
    }
}

