package com.lin_jp.kotlin

/**
 * 树结点3要素：父结点，子列表，是否展开
 */
class TreeNode(val value: String) {
    var isSelected: Boolean = false//选中
    var parent: TreeNode? = null//父结点
    val children: MutableList<TreeNode> = mutableListOf()//子结点列表
    val count: Int get() = getCountI(this, 0)//可见子结点
    val level: Int get() = getLevelI(this, -1)//树的层级
    var isExpanded: Boolean = false //是否展开
        set(expanded) {
            field = expanded && children.isNotEmpty()
        }// 必需要有子结点才可以设置为   true
    /**
     * 获取路径
     * @param root
     */
    fun getPath(root: TreeNode): String {
        return getPathI(this, root)
    }
    /**
     * 添加子结点
     * @param treeNode 要加入的子结点
     * @param  index 指定插入位置 负数为不指定
     */
    fun addChild(treeNode: TreeNode, index: Int) {
        treeNode.parent = this
        if (index < 0) children.add(treeNode)
        else children.add(index, treeNode)
    }
    /**
     * 删除当前结点
     */
    fun delete() {
        parent!!.children.remove(this)
        if (parent!!.children.isEmpty()) parent!!.isExpanded = false
    }
    /**
     * @param parentNode 移动当前结点到该结点
     * @param index 指定位置
     */
    fun move(parentNode: TreeNode, index: Int) {
        delete()
        parentNode.addChild(this, index)
    }
    /**
     * @param position 询问下标
     * @return 当前结点中对应的结点（可见结点）
     */
    fun getItem(position: Int): TreeNode {
        return getItemI(position, this)[1] as TreeNode
    }
    /**
     * @param child 询问的结点
     * @return 当前结点中的位置（可见结点）
     */
    fun indexOf(child: TreeNode): Int {
        return indexOfI(this, child, -1)[2] as Int
    }
    /**
     * 勾选子结点，遍历结点下的所有子结点
     */
    fun selectedToggle() {
        selectedToggleI(this, !isSelected)
    }
    /**
     * 勾选子结点后，刷新父结点
     * @param root 根结点
     *@return 需要刷新的父结点，在根结点中的下标
     */
    fun refreshParent(root: TreeNode): MutableList<Int> {
        return refreshParentI(this, root, mutableListOf())
    }

    private fun getPathI(child: TreeNode, root: TreeNode): String {
        return if (child.parent != root) "${getPathI(child.parent!!, root)}>${child.value}" else child.value
    }

    private fun refreshParentI(child: TreeNode, root: TreeNode, memory: MutableList<Int>): MutableList<Int> {
        if (child.parent != root) {//递归终点
            for (c in child.parent!!.children)
                if (!c.isSelected && !c.parent!!.isSelected) return memory//选中（所有子都是true）相反（父结点必需是true）
            child.parent!!.isSelected = child.isSelected//修改值
            memory.add(root.indexOf(child.parent!!))//把需要刷新的下标记录
            refreshParentI(child.parent!!, root, memory)//递归
        }
        return memory
    }

    private fun selectedToggleI(root: TreeNode, isChild: Boolean) {
        root.isSelected = isChild
        for (child in root.children)
            selectedToggleI(child, isChild)
    }

    private fun indexOfI(node: TreeNode, nodes: TreeNode, index: Int): List<Any> {
        var obj = listOf(node, nodes, index)
        if (node.isExpanded)
            for (cell in node.children)
                if (obj[0] == obj[1]) return obj
                else obj = indexOfI(cell, obj[1] as TreeNode, obj[2] as Int + 1)
        return obj
    }

    private fun getCountI(root: TreeNode, i: Int): Int {
        var i2 = i
        if (root.isExpanded)
            for (child in root.children)
                i2 = getCountI(child, i2 + 1)
        return i2
    }

    private fun getLevelI(child: TreeNode, i: Int): Int {
        return if (child.parent == null) i else getLevelI(child.parent!!, i + 1)
    }

    private fun getItemI(index: Int, node: TreeNode): List<Any> {
        var obj = listOf(index, node)
        if (node.isExpanded)
            for (child in node.children)
                if (obj[0] == -1) return obj
                else obj = getItemI(obj[0] as Int - 1, child)
        return obj
    }
}