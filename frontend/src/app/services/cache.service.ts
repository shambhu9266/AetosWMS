import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

interface CacheItem {
  data: any;
  timestamp: number;
  expiry: number;
}

@Injectable({
  providedIn: 'root'
})
export class CacheService {
  private cache = new Map<string, CacheItem>();
  private cacheStats = new BehaviorSubject({
    hits: 0,
    misses: 0,
    size: 0
  });

  private readonly DEFAULT_TTL = 5 * 60 * 1000; // 5 minutes

  constructor() {
    // Clean expired items every minute
    setInterval(() => this.cleanExpiredItems(), 60000);
  }

  set(key: string, data: any, ttl: number = this.DEFAULT_TTL): void {
    const item: CacheItem = {
      data,
      timestamp: Date.now(),
      expiry: Date.now() + ttl
    };
    
    this.cache.set(key, item);
    this.updateStats();
  }

  get(key: string): any | null {
    const item = this.cache.get(key);
    
    if (!item) {
      this.incrementMisses();
      return null;
    }
    
    if (Date.now() > item.expiry) {
      this.cache.delete(key);
      this.incrementMisses();
      this.updateStats();
      return null;
    }
    
    this.incrementHits();
    return item.data;
  }

  has(key: string): boolean {
    const item = this.cache.get(key);
    if (!item) return false;
    
    if (Date.now() > item.expiry) {
      this.cache.delete(key);
      this.updateStats();
      return false;
    }
    
    return true;
  }

  delete(key: string): boolean {
    const deleted = this.cache.delete(key);
    this.updateStats();
    return deleted;
  }

  clear(): void {
    this.cache.clear();
    this.updateStats();
  }

  getStats(): Observable<any> {
    return this.cacheStats.asObservable();
  }

  private cleanExpiredItems(): void {
    const now = Date.now();
    let cleaned = 0;
    
    for (const [key, item] of this.cache.entries()) {
      if (now > item.expiry) {
        this.cache.delete(key);
        cleaned++;
      }
    }
    
    if (cleaned > 0) {
      this.updateStats();
    }
  }

  private incrementHits(): void {
    const stats = this.cacheStats.value;
    this.cacheStats.next({
      ...stats,
      hits: stats.hits + 1
    });
  }

  private incrementMisses(): void {
    const stats = this.cacheStats.value;
    this.cacheStats.next({
      ...stats,
      misses: stats.misses + 1
    });
  }

  private updateStats(): void {
    const stats = this.cacheStats.value;
    this.cacheStats.next({
      ...stats,
      size: this.cache.size
    });
  }

  // Cache key generators
  static generateKey(prefix: string, ...params: any[]): string {
    return `${prefix}:${params.join(':')}`;
  }

  // Predefined cache keys
  static readonly KEYS = {
    USER_PROFILE: 'user:profile',
    DASHBOARD_STATS: 'dashboard:stats',
    REQUISITIONS: 'requisitions',
    NOTIFICATIONS: 'notifications',
    DEPARTMENTS: 'departments',
    USERS: 'users'
  };
}
