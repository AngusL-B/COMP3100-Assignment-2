# COMP3100-Assignment-2

ds-client simulator that uses a custom algorithm to assign jobs to servers

This program is a client side simulator that performs job dispatching based on a custom algorithm designed to optimise Turnaround Time, without sacrificing too much in the way of Resource Utilisation and Total Rental Cost

The algorithm has three passes to assign jobs (performing the next pass if the previous failed to find a server):
The first pass will assign the job to first available server that can run the job immediately
The second pass will assign the job to first server with the lowest amount of cores remaining after assigning the job there (assuming that the job can fit ie, a non negative amount of cores remaining)
The third pass will assign the job to the first server that does not have a job waiting in its queue

Under high job to server loads, these three passes can fail to find any applicable server, and if this happens that job is added to the global queue to be assigned later.