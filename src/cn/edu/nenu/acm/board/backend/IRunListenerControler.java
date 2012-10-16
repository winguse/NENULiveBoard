package cn.edu.nenu.acm.board.backend;

public interface IRunListenerControler {
	/**
	 * 返回会话开始没有
	 * @return
	 */
	public boolean isBegined();
	/**
	 * 开始监听，先进行一些初始化，清空之前都数据
	 */
	public void begin() ;
	/**
	 * 结束监听，清理数据
	 */
	public void terminate();
	/**
	 * 获得当前监听会话都开始时间
	 * @return
	 */
	public long getBvBeginFetchTime();
	/**
	 * 获得当前会话的最后更新时间
	 * @return
	 */
	public long getBvLastUpdateTime();
}
