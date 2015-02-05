package dcp.gui.pivot.facades;

import java.util.TreeMap;

import org.apache.pivot.collections.List;
import org.apache.pivot.wtk.content.TreeBranch;
import org.apache.pivot.wtk.content.TreeNode;

import dcp.config.io.IOFactory;
import dcp.logic.factory.TypeFactory.FILE_TYPE;
import dcp.logic.factory.CastFactory;
import dcp.logic.factory.GroupFactory;
import dcp.logic.factory.PackFactory;
import dcp.logic.model.Group;
import dcp.logic.model.Pack;
import dcp.main.log.Out;


public class SetFacade
{
    // DATA
    private List<TreeBranch> treeData; // Groups UI collection
    private java.util.Map<Group, TreeBranch> branches; // Treeview Branch mapped to Group
    private List<Pack> packs = PackFactory.getPacks(); // Packs
    
    /**
     * Set Tab Facade Constructor
     * @param treeData: TreeView data
     */
    public SetFacade(List<TreeBranch> treeData)
    {
        this.treeData = treeData;
        branches = new TreeMap<Group, TreeBranch>();
    }

    /**
     * Get Pack of a node
     * @param node to cast
     * @return Pack
     */
    public Pack getPack(TreeNode node)
    {
        for(Pack P:packs) {
            if (P.getGroup() != null && P.equals(node))
                return P;
        }
        return null;
    }
    
    /**
     * Create a new Pack model
     * @return pack created
     */
    public boolean newPack(Pack pack)
    {
        // Data update
        if (!PackFactory.addPack(pack))
            return false;
        // TreeView update
        if (pack.getGroup() != null)
            addNodeToBranch(pack);
        
        return true;
    }
    
    /**
     * Check if name is unique or already given to another pack
     * @param pack name
     * @return pack name is unique
     */
    public boolean validatePack(String name)
    {
        int i = 0;
        for(Pack p:packs) {
            if (p.getInstallName().equals(name)) i++;
        }
        if (i > 1) return false;
        return true;
    }
    
    /**
     * Delete a pack from memory
     * @param pack to delete
     */
    public boolean deletePack(Pack pack)
    {
        if (pack.getGroup() != null)
            return false;
        return PackFactory.removePack(pack);
    }
    
    /**
     * Clear all packs from memory
     */
    public void clearPacks()
    {
        PackFactory.clear();
    }
    
    ////=====================================================================================

    /**
     * Get Group of a branch
     * @param branch to cast
     * @return Group
     */
    public Group getGroup(TreeBranch branch) {
        return GroupFactory.get(CastFactory.nodeToPath(branch));
    }
    
    /**
     * Add a new group under an existing group or at TreeView root
     * @param name of new group
     * @param parent of new group
     */
    public boolean newGroup(String name, String path)
    {
        Group parent = GroupFactory.get(path);
        
        if (parent != null) {
            Group group = new Group(name, parent);
            if (GroupFactory.addGroup(group)) {
                TreeBranch branch = CastFactory.groupToSingleBranch(group);
                branches.get(parent).add(branch);
                
                branches.put(group, branch);
                Out.print("MODEL", "Group added: " + group.getPath());
                return true;
            }
            else return false;
        }
        else {
            Group group = new Group(name);
            if (GroupFactory.addGroup(group)) {
                TreeBranch branch = new TreeBranch(IOFactory.imgFolder, name);
                treeData.add(branch);
                
                branches.put(group, branch);
                Out.print("MODEL", "Group added: " + group.getPath());
                return true;
            }
            else return false;
        }
    }
    public boolean newGroup(Group group)
    {
        Group parent = group.getParent();
        return newGroup(group.getName(), (parent != null)? parent.getPath():"");
    }
    
    /**
     * Rename a group
     * @param group to rename
     * @param new_name to give
     */
    public void renameGroup(Group group, String new_name)
    {
        assert group != null;
        if (new_name.length() > 0) {
            String old_name = group.getName();
            branches.get(group).setText(new_name);
            group.setName(new_name);
            Out.print("MODEL", "Group "+old_name+" renamed to "+new_name);
        }
    }
    
    //Unlink packs and groups from the Group
    private void unlinkGroup(Group group) {
        for(Pack p:packs) {
            if (p.getGroup() != null && p.getGroup().equals(group))
                p.setGroup(null);
        }
        for(Group g:group.getChildren()) unlinkGroup(g);
    }
    /**
     * Remove a Group or Pack from TreeView
     * @param group to remove
     */
    public void removeNode(TreeNode node)
    {
        assert node != null;
        
        if (node instanceof TreeBranch)// Group
        {
            TreeBranch branch = (TreeBranch) node;
            Group group = getGroup(branch);
            String path = group.getPath();
            assert group != null;
            unlinkGroup(group);
            GroupFactory.removeGroup(group);
            //TreeBranch remove
            if (branch.getParent() != null)
                branch.getParent().remove(branch);
            else treeData.remove(branch);
            Out.print("MODEL", "Group removed: " + path);
        }
        else if (node instanceof TreeNode)// Pack
        {
            TreeBranch branch = node.getParent();
            String name = getGroup(branch).getName();
            Pack P = getPack(node);
            P.setGroup(null);
            //TreeNode remove
            branch.remove(node);
            Out.print("PIVOT_SET", "Pack '" + P.getName() + "' removed from Group: "+name);
        }
    }
    
    /**
     * Clear groups from memory and treeView data
     */
    public void clearGroups()
    {
        GroupFactory.clear();
        treeData.clear();
    }
    
    /**
     * Clear nodes from TreeView
     */
    public void clearTreeView()
    {
        while (treeData.getLength() > 0) {
            removeNode(treeData.get(0));
        }
    }
    
    ////=====================================================================================
    
    // add a pack's node to its group's branch in treeView data
    private void addNodeToBranch(Pack pack)
    {
        assert pack != null && pack.getGroup() != null;
        Group group = pack.getGroup();
        
        TreeNode node = CastFactory.packToNode(pack);//Create Pack Node
        TreeBranch branch = branches.get(group);//Get Group branch
        
        //If pack is a folder (bugfix)
        if (pack.getFileType() == FILE_TYPE.Folder)// && pack.getInstallType()==INSTALL_TYPE.COPY)
            for(TreeNode n:branch)//Check if a group is already created with same name
                if (n instanceof TreeBranch && n.getText().equalsIgnoreCase(node.getText())) {
                    GroupFactory.removeGroup(getGroup((TreeBranch) n));//Remove model
                    branch.remove(n);//Remove group branch to overwrite it with folder Pack
                    break;
                }
        
        branch.add(node);// Add node to branch
        Out.print("MODEL", "Pack '" + pack.getName() + "' added to Group: " + group.getPath());
    }
    
    /**
     * Add pack to group
     * @param pack
     * @param group
     * @return (0-no error; 1-already in a group; 2-dependent on that group)
     */
    public int addPackToGroup(Pack pack, Group group)
    {
        assert pack != null && group != null;
        if (pack.getGroup() == null) {//If Pack has no group
            //If pack dependent on this group or parent
            if (pack.getGroupDependency()!=null && group.hasParent(pack.getGroupDependency())) {
                return 2;
            }
            else {
                pack.setGroup(group);//Link Pack with Group
                addNodeToBranch(pack);
                return 0;
            }
        }
        else {//If Pack already has Group
            return 1;
        }
    }
    
    /**
     * Get a suggestion for a path based on other install paths and groups
     * @param text: suggestion's prefix
     * @return complete suggestion
     */
    public String getInstallPathSuggestion(String text)
    {
        for(Group G:GroupFactory.getGroups()) {//Suggestions from Group paths
            if (G.getPath().toLowerCase().startsWith(text)) {
                return G.getPath();
            }
        }
        //Suggestions from already attributed install paths to Packs
        for(Pack P:PackFactory.getPacks()) {
            if (P.getInstallPath().toLowerCase().startsWith(text)) {
                return P.getInstallPath();
            }
        }
        
        return "";
    }
    
}