/* Copyright (C) 2009 Registro.br. All rights reserved. 
* 
* Redistribution and use in source and binary forms, with or without 
* modification, are permitted provided that the following conditions are 
* met:
* 1. Redistribution of source code must retain the above copyright 
*    notice, this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in the
*    documentation and/or other materials provided with the distribution.
* 
* THIS SOFTWARE IS PROVIDED BY REGISTRO.BR ``AS IS'' AND ANY EXPRESS OR
* IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIE OF FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
* EVENT SHALL REGISTRO.BR BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
* BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
* OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
* TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
* USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
* DAMAGE.
 */
package br.registro.dnsshim.domain.logic;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public abstract class CacheStrategy<K, V extends Cacheable<K>> {
	private final Logger logger = Logger.getLogger(this.getClass());// pera pegar a subclasse
	
	// Least Recently Used (LRU): discards the least recently used items first.
	private final Map<K,V> cache = new LinkedHashMap<K,V>(getMaxEntriesInCache(), 0.75F, true) {
		protected boolean removeEldestEntry(java.util.Map.Entry<K,V> eldest) {
			if (getMaxEntriesInCache() == 0) {
				return false;
			}
			return this.size() > getMaxEntriesInCache();
		}
	};
	
	protected Map<K,V> getCache() {
		return cache;
	}
	
	public synchronized V remove(K k) {
		if (logger.isTraceEnabled()) {
			logger.trace("Removing object from cache");
		}
		return cache.remove(k);
	}	

	public synchronized boolean contains(K k) {
		if (get(k) == null) {			
			return false;
		} else {		
			return true;
		}
	}
	
	public synchronized V get(K k) {
		V cached = cache.get(k);
		if (cached == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Object not found in cache. Asking cache strategy...");
			}
			V value = restoreValue(k);
			if (value != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Object restored from cache strategy.Putting in cache");
				}
				addCached(value);
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace("Object not found by cache strategy.");
				}
			}
			return value;
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace("Object found in cache.");
			}
			return cached;
		}
	}
	
	public synchronized void put(V v) {
		if (logger.isTraceEnabled()) {
			logger.trace("Adding new object in cache. Calling store strategy...");
		}
		storeValue(v);
		addCached(v);
	}
	
	public synchronized Set<K> keySet() {
		return new HashSet<K>(cache.keySet());
	}
	
	public synchronized Set<Entry<K,V>> entrySet() {
		return new HashSet<Entry<K, V>>(cache.entrySet());
	}
	
	protected synchronized void addCached(V v){
		cache.put(v.getCacheKey(), v);
		
		if (logger.isTraceEnabled()) {
			logger.trace("Actual cache size = "+cache.size());
			logger.trace("Max (0=unlimited) cache size = "+getMaxEntriesInCache());
		}
	}
	
	public int size() {
		return cache.size();
	}
	
	protected abstract V restoreValue(K k);
	protected abstract void storeValue(V v);

	/**
	 * 0 for unlimited
	 */
	protected abstract int getMaxEntriesInCache();

}