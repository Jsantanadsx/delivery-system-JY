#!/usr/bin/env python3
"""Send 50 real HTTP requests to order-service. Use a fresh promotion stock=10."""
import concurrent.futures
import json
import sys
import threading
from collections import Counter
from urllib import error, request

URL = sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8080'
THREADS = 50
barrier = threading.Barrier(THREADS)

def send(_):
    barrier.wait()
    body = json.dumps({'dishId': 1, 'quantity': 1}).encode('utf-8')
    req = request.Request(URL + '/orders', data=body,
                          headers={'Content-Type': 'application/json'}, method='POST')
    try:
        with request.urlopen(req, timeout=90) as response:
            return response.status
    except error.HTTPError as ex:
        return ex.code
    except Exception as ex:
        return 'network-error:' + type(ex).__name__

if __name__ == '__main__':
    print('Run this against a FRESH database where dish 1 stock is 10.')
    with concurrent.futures.ThreadPoolExecutor(max_workers=THREADS) as pool:
        results = list(pool.map(send, range(THREADS)))
    counts = Counter(results)
    print('HTTP results:', dict(counts))
    with request.urlopen(URL + '/dishes/1', timeout=10) as response:
        dish = json.load(response)
    print('Final promotion stock:', dish['stock'])
    assert dish['stock'] >= 0, 'Negative stock: lock not working'
    assert counts.get(201, 0) <= 10, 'Oversold stock!'
    if counts.get(201, 0) == 10 and dish['stock'] == 0:
        print('PASS: exactly 10 confirmed orders with no overselling')
    else:
        print('Inconclusive: random payment failures or previous orders may affect the total.')
        print('For a deterministic race test, run :order-service:test.')
