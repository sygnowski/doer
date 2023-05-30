


### Notes

Source https://helix.apache.org/1.0.2-docs/tutorial_state.html


Dynamic State Constraints
We also support two dynamic upper bounds for the number of replicas in each state:
 - N: The number of replicas in the state is at most the number of live participants in the cluster
 - R: The number of replicas in the state is at most the specified replica count for the partition


```bash
doer helix -c doer-cluster-test -t rebalance -r doer --replicas 3

```