package com.lin_jp.kotlin

import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.widget.Toast
import kotlinx.android.synthetic.main.ttee_item.view.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fun getVibrator() = getSystemService(VIBRATOR_SERVICE) as Vibrator//注入取震动对象方法
        thee_view.layoutManager = LinearLayoutManager(this)
        thee_view.setHasFixedSize(true)//高度不变时可提高效率
        thee_view.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            val rootNode: TreeNode = TreeNode("根结点")//根结点不显
            var listener: View.OnClickListener = View.OnClickListener { v ->
                val child = v.tag as TreeNode
                when (v.id) {
                    R.id.check -> {//CheckBox联动
                        child.selectedToggle()
                        if (child.isExpanded) //刷新子结点
                            notifyItemRangeChanged(rootNode.indexOf(child) + 1, child.count, "CheckBox")
                        for (i in child.refreshParent(rootNode))
                            notifyItemChanged(i, "CheckBox")
                    }
                    else -> {
                        Toast.makeText(this@MainActivity, child.getPath(rootNode), Toast.LENGTH_SHORT).show()
                        if (child.children.isNotEmpty())//展开或关闭子结点
                            if (child.isExpanded) closeNode(child)
                            else openNode(child)
                    }
                }
            }//开关子结点和勾选联动
            var colorArray: Array<Int?>

            init {
                var x = 0
                for (i in 0..4) {
                    val child1 = TreeNode("菜单${x++}")
                    rootNode.addChild(TreeNode("菜单${x++}"), -1)
                    rootNode.addChild(child1, -1)
                    for (j in 0..2) {
                        val child2 = TreeNode("菜单${x++}")
                        child1.addChild(TreeNode("菜单${x++}"), -1)
                        child1.addChild(child2, -1)
                        for (k in 0..1) {
                            val child3 = TreeNode("菜单${x++}")
                            child2.addChild(child3, -1)
                            for (l in 0..1) {
                                val child4 = TreeNode("菜单${x++}")
                                child3.addChild(child4, -1)
                                for (m in 0..1)
                                    child4.addChild(TreeNode("菜单${x++}"), -1)
                            }
                        }
                    }
                }//组织模拟数据
                rootNode.isExpanded = true//展开根结点
                resources.obtainTypedArray(R.array.clor_list).run {
                    colorArray = arrayOfNulls(length())
                    for (i in 0 until length()) colorArray[i] = getColor(i, 0)
                    recycle()//回收
                }//取出颜色列表
                ItemTouchHelper(object : ItemTouchHelper.Callback() {
                    override fun isItemViewSwipeEnabled(): Boolean {
                        return true
                    }   //是否支持划动

                    override fun isLongPressDragEnabled(): Boolean {
                        return true  // mItemHelper.startDrag(RecyclerView.ViewHolder);//也可以调此方法触发
                    }    //是否长按触发

                    override fun getMovementFlags(view: RecyclerView, holder: RecyclerView.ViewHolder): Int {
                        return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT)
                    }//拖拽的方向和划动方向

                    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                        super.clearView(recyclerView, viewHolder)
                        val node = viewHolder.itemView.tag as TreeNode
                        if (node.parent!!.children.contains(node)) {//包含表示非删除
                            viewHolder.itemView.alpha = 1f//还原
                            viewHolder.itemView.scaleY = 1f
                        }
                    }//最终的状态

                    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                        super.onSelectedChanged(viewHolder, actionState)
                        if (viewHolder != null) {
                            val child = viewHolder.itemView.tag as TreeNode
                            if (child.isExpanded) closeNode(child)//不管是拖拽还是删除发现打开都先关闭
                            if (ItemTouchHelper.ACTION_STATE_DRAG == actionState) {//长按移动
                                getVibrator().vibrate(99)//震动 安卓8.0 以后写法： getVibrator().vibrate(VibrationEffect.createOneShot(99,99))
                                viewHolder.itemView.alpha = 0.8f//透明度
                                viewHolder.itemView.scaleY = 1.08f//放大y
                            }
                        }
                    }  //选中以后回调的方法

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val child = rootNode.getItem(viewHolder.adapterPosition)
                        child.delete()
                        deleteImg(child.parent!!, false)
                        notifyItemRemoved(viewHolder.adapterPosition)
                        checkParent(child.parent!!, child.isSelected, true)
                    }//划动完成以后会回调的方法

                    override fun onMove(
                        view: RecyclerView,
                        from: RecyclerView.ViewHolder,
                        to: RecyclerView.ViewHolder
                    ): Boolean {
                        val fromNode = rootNode.getItem(from.adapterPosition)
                        val toNode = rootNode.getItem(to.adapterPosition)
                        val index = toNode.parent!!.children.indexOf(toNode)
                        val fp: TreeNode? = fromNode.parent//移动前记录父结点（移动后父结点就变了）
                        val isUp = from.adapterPosition > to.adapterPosition//是否向上移动
                        when {
                            isUp -> fromNode.move(toNode.parent!!, index)
                            toNode.count == 0 -> fromNode.move(
                                toNode.parent!!,
                                if (fromNode.level == toNode.level) index else index + 1
                            )
                            else -> fromNode.move(toNode, 0)
                        }
                        deleteImg(fp!!, isUp)
                        notifyItemMoved(from.adapterPosition, to.adapterPosition)
                        if (fp != fromNode.parent) {//刷新父结点勾选状态
                            checkParent(fp, fromNode.isSelected, true)
                            checkParent(fromNode.parent!!, fromNode.isSelected, false)
                        }
                        return true
                    }//拖动的过程中不断回调的方法,在这里可以写上一些交换数据的逻辑
                }).attachToRecyclerView(thee_view) //可拖拽,可划动
            }//初始化模拟数据  和   加入拖拽更改位置和划动删除功能

            private fun checkParent(parent: TreeNode, isSelect: Boolean, isMinus: Boolean) {
                if (!isSelect)
                    for (cell in parent.children)
                        if (cell.isSelected == isMinus) {
                            for (i in cell.refreshParent(rootNode))
                                notifyItemChanged(i, "CheckBox")
                            return
                        }
            }//移动和删除时刷新父结点的勾选状态

            private fun deleteImg(parent: TreeNode, isUp: Boolean) {
                if (!parent.isExpanded) {
                    var index = rootNode.indexOf(parent)
                    if (isUp) index -= 1
                    notifyItemChanged(index, "deleteImg")
                }
            }//当没有子结点时隐藏父结点的展开标记

            private fun closeNode(child: TreeNode) {
                val cont = child.count
                child.isExpanded = false
                notifyItemRangeRemoved(rootNode.indexOf(child) + 1, cont)//批量移除子结点
                notifyItemChanged(rootNode.indexOf(child), "ImageView")//父结点前箭标动画
            }//关闭子结点

            private fun openNode(child: TreeNode) {
                child.isExpanded = true
                notifyItemRangeInserted(rootNode.indexOf(child) + 1, child.count)//批量插入子结点
                notifyItemChanged(rootNode.indexOf(child), "ImageView")//父结点前箭标动画
            }//打开子结点

            override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): RecyclerView.ViewHolder {
                val viewHolder = object : RecyclerView.ViewHolder (layoutInflater.inflate(R.layout.ttee_item, viewGroup, false)){}
                (viewHolder.itemView.arrow_img.layoutParams as ConstraintLayout.LayoutParams).setMargins(type * 50, 0, 0, 0)
                viewHolder.itemView.background =
                    RippleDrawable(ColorStateList.valueOf(Color.WHITE), ColorDrawable(colorArray[type]!!), null)
                viewHolder.itemView.setOnClickListener(listener)
                viewHolder.itemView.check.setOnClickListener(listener)
                return viewHolder
            }//创建View

            override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {//根据标记刷新局部View
                if (payloads.isEmpty()) onBindViewHolder(viewHolder, position)
                else {
                    val itemData = rootNode.getItem(position)
                    for (obj in payloads)
                        when (obj.toString()) {
                            "ImageView" -> viewHolder.itemView.arrow_img.animate().rotation(if (itemData.isExpanded) 90f else 0f).setDuration(200).start()//父结点前的箭标 开关动画旋转
                            "deleteImg" -> viewHolder.itemView.arrow_img.visibility = View.INVISIBLE     //隐藏父结点前的箭标
                            "CheckBox" -> viewHolder.itemView.check.isChecked = itemData.isSelected    //更新选中状态
                        }
                }
            }

            override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
                val itemData = rootNode.getItem(position)
                viewHolder.itemView.tag = itemData
                viewHolder.itemView.check.tag = itemData

                viewHolder.itemView.name.text = itemData.value
                viewHolder.itemView.check.isChecked = itemData.isSelected
                if (itemData.children.isEmpty()) {
                    viewHolder.itemView.arrow_img.visibility = View.INVISIBLE
                } else {
                    viewHolder.itemView.arrow_img.visibility = View.VISIBLE
                    viewHolder.itemView.arrow_img.rotation = if (itemData.isExpanded) 90f else 0f
                }
            }

            override fun getItemViewType(position: Int): Int {
                return rootNode.getItem(position).level
            }//用层级分类

            override fun getItemCount(): Int {
                return rootNode.count
            }
        }
    }
}
