/**
 * (C) 2011-2012 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 */
package com.taobao.profile.analysis;

import com.taobao.profile.utils.MathUtils;

import java.io.*;
import java.util.*;

/**
 * 分析Profiler生成的Log
 *
 * @author shutong.dy
 * @since 2012-1-11
 */
public class ProfilerLogAnalysis {

	private String logPath;
	private String methodPath;
	private boolean nano = false;
	private long currentthreadId = -1;
	private List<MethodStack> threadList = new ArrayList<MethodStack>();
	private Map<Long, TimeSortData> cacheMethodMap = new HashMap<Long, TimeSortData>();
	private Map<Long, String> methodIdMap = new HashMap<Long, String>();

	private Map<Long,TransactionTimeSortData> transactionList = new HashMap<Long,TransactionTimeSortData>();

	private long transactionMethodId;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 5) {
			System.err.println("Usage: <tprofiler.log path> <tmethod.log path> <topmethod.log path> <topobject.log path> <toptransaction.log path>");
			return;
		}
		ProfilerLogAnalysis analysis = new ProfilerLogAnalysis(args[0], args[1]);
		analysis.reader();
		analysis.printResult(args[2], args[3], args[4]);
	}

	/**
	 * @param inPath
	 * @param methodPath
	 */
	public ProfilerLogAnalysis(String inPath, String methodPath) {
		this.logPath = inPath;
		this.methodPath = methodPath;
	}

	/**
	 * 取出结果,供分析程序调用
	 *
	 * @return
	 */
	public List<TimeSortData> getTimeSortData() {
		List<TimeSortData> list = new ArrayList<TimeSortData>();
		list.addAll(cacheMethodMap.values());
		Collections.sort(list);
		return list;
	}

	/**
	 * 读取log,并解析
	 */
	private void reader() {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(methodPath));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("instrument")) {
					continue;
				}
				String[] data = line.split(" ");
				if (data.length != 2) {
					continue;
				}

				if (line.contains("invokeWithinTransaction")) {
					transactionMethodId = Long.parseLong(data[0]);
				}

				methodIdMap.put(Long.parseLong(data[0]), String.valueOf(data[1]));
			}
			reader.close();

			reader = new BufferedReader(new FileReader(logPath));
			line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("##")) {
					line = line.substring(line.indexOf(":") + 1, line.length());
					if (line.equals("true")) {
						nano = true;
					} else {
						nano = false;
					}
					continue;
				}
				if ("=".equals(line)) {
					currentthreadId = -1;
					doMerge();
				}
				String[] data = line.split("\t");
				if (data.length != 4) {
					continue;
				}

				pushThreadInfo(Long.parseLong(data[0]), Long.parseLong(data[1]), Long.parseLong(data[2]),
						Long.parseLong(data[3]));

				merge(Long.parseLong(data[0]), Long.parseLong(data[1]), Long.parseLong(data[2]),
						Long.parseLong(data[3]));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		doMerge();
	}

	private void pushThreadInfo(long threadid, long stackNum, long methodId, long useTime) {
		if (transactionList.get(threadid) == null) {
			TransactionTimeSortData d = new TransactionTimeSortData(threadid, useTime, new ArrayList<Long>(Arrays.asList(methodId)));
				transactionList.put(threadid, d);
		}
		TransactionTimeSortData transactionTimeSortData = transactionList.get(threadid);
		transactionTimeSortData.getMethodIdList().add(methodId);
		if (methodId == transactionMethodId) {
			transactionTimeSortData.setTime(useTime);
		}


	}

	/**
	 * 合并数据
	 *
	 * @param threadid
	 * @param stackNum
	 * @param methodId
	 * @param useTime
	 */
	private void merge(long threadid, long stackNum, long methodId, long useTime) {
		if (currentthreadId != threadid) {
			currentthreadId = threadid;
			doMerge();
		}
		MethodStack m = new MethodStack();
		m.methodId = methodId;
		m.useTime = useTime;
		m.stackNum = stackNum;
		threadList.add(m);
	}

	/**
	 * 合并数据
	 */
	private void doMerge() {
		for (int i = 0; i < threadList.size(); i++) {
			MethodStack m = threadList.get(i);
			long statck = m.stackNum;
			for (int j = i + 1; j < threadList.size(); j++) {
				MethodStack tmp = threadList.get(j);
				long tmpStack = tmp.stackNum;
				if (statck + 1 == tmpStack) {
					m.useTime -= tmp.useTime;
				} else if (statck >= tmpStack) {
					break;
				}
			}
		}
		for (int i = 0; i < threadList.size(); i++) {
			MethodStack m = threadList.get(i);
			if (m.useTime < 0) {
				break;
			}
			TimeSortData sortData = cacheMethodMap.get(m.methodId);
			if (sortData == null) {
				sortData = new TimeSortData();
				sortData.setMethodName(methodIdMap.get(m.methodId));
				sortData.addStackValue(m.useTime);
				cacheMethodMap.put(m.methodId, sortData);
			} else {
				sortData.addStackValue(m.useTime);
			}
		}
		threadList.clear();
	}

	/**
	 * 输出分析结果
	 */
	public void printResult(String topMethodPath, String topObjectPath, String topTransactionPath) {
		List<TimeSortData> list = new ArrayList<TimeSortData>();
		list.addAll(cacheMethodMap.values());
		Collections.sort(list);

		BufferedWriter topMethodWriter = null;
		BufferedWriter topObjectWriter = null;
		BufferedWriter topTransactionWriter = null;
		List<TransactionTimeSortData> tl = new ArrayList();
		tl.addAll(transactionList.values());
		Collections.sort(tl);


		try {
			topMethodWriter = new BufferedWriter(new FileWriter(topMethodPath));
			topObjectWriter = new BufferedWriter(new FileWriter(topObjectPath));
			topTransactionWriter = new BufferedWriter(new FileWriter(topTransactionPath));

			for (TransactionTimeSortData sortData : tl) {
				StringBuilder sb = new StringBuilder();
				List<Long> methodList = sortData.getMethodIdList();

				sb.append(sortData.getThreadId());
				sb.append("\t");
				sb.append(sortData.getTime());
				sb.append("\n");
				for (Long methodId : methodList) {
					sb.append(methodIdMap.get(methodId));
					sb.append("\n");
				}
				sb.append("----------------------");
				sb.append("\n");
				topTransactionWriter.write(sb.toString());

			}

			for (TimeSortData data : list) {
				StringBuilder sb = new StringBuilder();
				Stack<Long> stack = data.getValueStack();

				long executeNum = stack.size();
				long allTime;
				if (nano) {
					allTime = MathUtils.div(data.getSum(), 1000000);
				} else {
					allTime = data.getSum();
				}
				long useTime = MathUtils.div(allTime, executeNum);
				sb.append(data.getMethodName());
				sb.append("\t");
				sb.append(executeNum);
				sb.append("\t");
				sb.append(useTime);
				sb.append("\t");
				sb.append(allTime);
				sb.append("\n");
				topMethodWriter.write(sb.toString());
				if (data.getMethodName() != null && data.getMethodName().contains("<init>")) {
					topObjectWriter.write(sb.toString());
				}
			}
			topTransactionWriter.flush();
			topMethodWriter.flush();
			topObjectWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (topMethodWriter != null) {
				try {
					topMethodWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (topObjectWriter != null) {
				try {
					topObjectWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (topTransactionWriter != null) {
				try {
					topTransactionWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 方法栈
	 *
	 * @author shutong.dy
	 * @since 2012-1-11
	 */
	private class MethodStack {
		private long methodId;
		private long useTime;
		private long stackNum;
	}
}
