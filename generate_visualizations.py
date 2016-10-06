# -*- coding: utf-8 -*-

import matplotlib.pyplot as plt
import csv

def plot_graphs():
    clients = []
    clientCache = []
    ticks = []
    localHits = []
    neighborHits = []
    cacheMisses = []
    serverMemory = []

    with open('config_num_clients_LRU.properties_output.txt','rU') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            clients.append(int(row[0]))
            clientCache.append(int(row[1]))
            ticks.append(int(row[2]))
            localHits.append(int(row[3]))
            neighborHits.append(int(row[4]))
            cacheMisses.append(int(row[5]))
#             serverMemory.append(int(row[6]))
    
    plt.figure(0)
    plt.plot(clients,ticks)
    plt.xlabel('Number of Clients')
    plt.ylabel('number of ticks')
    plt.savefig('graphs/cacheSize_ticks.png')
    
    plt.figure(1)
    plt.plot(clients,cacheMisses)
    plt.xlabel('Number of Clients')
    plt.ylabel('cache misses')
    plt.savefig('graphs/cacheSize_misses.png')
    
    plt.figure(2)
    plt.plot(clients,localHits,label="Local Hits")
    plt.plot(clients,neighborHits,label="Neighbor Hits")
    plt.xlabel('Number of Clients')
    plt.ylabel('cache hits')
    plt.legend()
    plt.savefig('graphs/cacheSize_hits.png')


plot_graphs()
