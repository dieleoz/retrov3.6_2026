import warnings; warnings.filterwarnings('ignore')
import sys, random
from collections import Counter
exec(open('brute.py').read().split('random.seed')[0])
random.seed(int(sys.argv[1])); N=int(sys.argv[2])
gens={'c2 unif +-1e-3':lambda: random.uniform(-1e-3,1e-3),
      'c2 log 1e-7..1e-3':lambda: random.choice((-1,1))*10**random.uniform(-7,-3),
      'c1 unif 0..2':lambda: random.uniform(0,2),
      'c0 unif +-1000':lambda: random.uniform(-1000,1000),
      'c3 log 1e-9..1e-6':lambda: random.choice((-1,1))*10**random.uniform(-9,-6)}
for name,g in gens.items():
    h=Counter(); w=None
    for k in range(N):
        a='%.8E'%g(); b=efmt(strtof(a)); env=F(float(a)); lei=F(float(b))
        e=round(abs(float(lei)-float(env))/ulp(lei),3); h[e]+=1
        if w is None or e>w[0]: w=(e,a,b)
    print(name, sorted(h.items()), 'peor', w)
